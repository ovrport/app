package moe.crx.overport.patching

import com.android.apksig.ApkSigner
import com.reandroid.apk.*
import com.reandroid.archive.ArchiveFile
import com.reandroid.archive.FileInputSource
import com.reandroid.archive.io.ZipFileInput
import moe.crx.overport.config.OverportPatchedInfo
import moe.crx.overport.utils.CantCheckoutException
import moe.crx.overport.utils.NameFormatter
import moe.crx.overport.versions.VersionManager
import org.jf.baksmali.Baksmali
import org.jf.baksmali.BaksmaliOptions
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.VersionMap
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.dexbacked.raw.HeaderItem
import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

class PatcherContext(
    private var overportVersion: String,
    val patcherDirectory: File,
    val inputFileName: String,
    val nameFormat: String,
) {
    val dataDir: File = patcherDirectory.resolve("dec")

    val namespace: String = "working"
    val inputApk: File = dataDir.resolve("$namespace.apk")
    val outputApk: File = dataDir.resolve("$namespace.output.apk")

    val workingDir: File = dataDir.resolve(namespace)
    var workingApk: ApkModule? = null

    fun isApkLoaded(): Boolean {
        return workingApk != null
    }

    fun overportVersion(): String {
        return overportVersion
    }

    fun outputFileName(): String {
        return NameFormatter.format(nameFormat, this)
    }

    fun checkout() {
        overportVersion = VersionManager(patcherDirectory).checkout(overportVersion)
            ?: throw CantCheckoutException("Can't checkout an overport release. Please, check your internet connection or choose another version.")
    }

    fun prepare(stream: InputStream) {
        dataDir.mkdirs()

        workingDir.deleteRecursively()
        inputApk.delete()
        outputApk.delete()

        Files.copy(stream, inputApk.toPath(), REPLACE_EXISTING)

        val zipInput = ZipFileInput(inputApk)
        val apk = runCatching {
            val archive = ArchiveFile(zipInput)
            ApkModule("base", archive.createZipEntryMap()).apply {
                apkSignatureBlock = archive.apkSignatureBlock
                setCloseable(archive)
            }
        }.onFailure {
            zipInput.close()
        }.getOrThrow()

        val decoder = ApkModuleJsonDecoder(apk).apply {
            dexDecoder = DexDecoder { dexFile, dir ->
                val bytes = ByteArrayOutputStream().apply {
                    dexFile.write(this)
                }.toByteArray()

                val version = HeaderItem.getVersion(bytes, 0)
                val api = VersionMap.mapDexVersionToApi(version)
                val opcodes = Opcodes.forApi(api)

                val classesName = dexFile.dexNumber.let { if (it != 0) "classes$it" else "classes" }
                val smaliDir = dir.resolve("smali").resolve(classesName)

                val options = BaksmaliOptions().apply {
                    localsDirective = true
                    sequentialLabels = true
                    skipDuplicateLineNumbers = true
                    apiLevel = api
                }
                Baksmali.disassembleDexFile(DexBackedDexFile(opcodes, bytes), smaliDir, 4, options)
            }
        }

        decoder.sanitizeFilePaths()
        decoder.decode(workingDir)

        workingApk = apk
    }

    fun patch(
        args: Map<String, List<String>>,
        callback: (Int, Patch) -> Unit = { _, _ -> }
    ) {
        val librariesDir = VersionManager(patcherDirectory).getLibraries(overportVersion)

        val executor = PatchExecutor(librariesDir, workingDir, apk())

        val patches = args.keys.mapNotNull { PatchStore.get(it) }

        patches.forEachIndexed { index, patch ->
            callback(index, patch)
            patch.executor(executor, args[patch.name] ?: listOf())
        }

        executor.selectConfig {
            patched = OverportPatchedInfo(
                by = VersionManager.VERSION,
                with = overportVersion,
                patches = patches.map { it.name }
            )
        }
    }

    fun export(stream: OutputStream) {
        val encoder = ApkModuleJsonEncoder().apply {
            dexEncoder = DexEncoder { _, dir ->
                dir
                    .resolve("smali")
                    .listFiles()
                    ?.filter { file -> file.isDirectory }
                    ?.map {
                        dir.resolve(".cache").mkdirs()
                        val file = dir.resolve(".cache").resolve("${it.name}.dex")

                        val smaliOptions = SmaliOptions().apply {
                            outputDexFile = file.absolutePath
                            jobs = 4
                            apiLevel = 29
                        }

                        Smali.assemble(smaliOptions, it.absolutePath)
                        FileInputSource(file, file.name)
                    }
                    ?.toList()
            }
        }

        encoder.scanDirectory(workingDir)
        encoder.apkModule.run {
            zipEntryMap.autoSortApkFiles()
            writeApk(outputApk)
            close()
        }

        val signatureDir = patcherDirectory.resolve("signatures")
        signatureDir.mkdirs()
        val signature = appPackage()
            .plus(".keystore")
            .let { signatureDir.resolve(it) }
            .let { SignatureStore.getSignature(it) }

        val tempFile = outputApk
            .parentFile
            ?.resolve(outputApk.name + ".temp")
            ?.let {
                it.delete()
                outputApk.copyTo(it)
            }

        ApkSigner
            .Builder(listOf(signature))
            .setAlignFileSize(true)
            .setMinSdkVersion(29)
            .setInputApk(tempFile)
            .setOutputApk(outputApk)
            .build()
            .sign()

        tempFile?.delete()

        Files.copy(outputApk.toPath(), stream)
    }

    fun cleanup() {
        workingDir.deleteRecursively()
        workingApk?.close()
        workingApk = null
        inputApk.delete()
        outputApk.delete()
    }

    private fun apk(): ApkModule {
        val apk = workingApk
        checkNotNull(apk)
        return apk
    }

    fun appPackage(): String {
        return apk().androidManifest.packageName
    }

    fun appVersionCode(): String {
        return apk().androidManifest.versionCode.toString()
    }

    fun appVersionName(): String {
        return apk().androidManifest.versionName
            ?: apk().androidManifest.versionCode.toString()
    }

    fun appName(): String {
        return apk().androidManifest.applicationLabelString
            ?: apk().tableBlock
                .getEntries(workingApk?.androidManifest?.applicationLabelReference ?: 0)
                .asSequence()
                .first()
                .valueAsString
    }

    fun appIcon(): File? {
        val iconId = apk().androidManifest.iconResourceId.takeIf { it != 0 }
            ?: apk().androidManifest.roundIconResourceId.takeIf { it != 0 }
            ?: return null

        val result = apk().listResFiles(iconId, null)
            ?: return null

        // TODO layered adaptive icons support

        arrayOf(
            "-xxxhdpi",
            "-xxhdpi",
            "-xhdpi",
            "-hdpi",
            "-mdpi",
            "-ldpi",
            "-anydpi",
            ""
        ).forEach { dpi ->
            arrayOf(".webp", ".png").forEach { ext ->
                result.firstOrNull { it.filePath.contains(dpi) && it.filePath.endsWith(ext) }?.let {
                    workingDir.resolve("root").resolve(it.filePath).run {
                        if (exists()) {
                            return this
                        }
                    }
                }
            }
        }

        return null
    }
}
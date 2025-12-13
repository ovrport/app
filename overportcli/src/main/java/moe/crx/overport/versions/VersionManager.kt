package moe.crx.overport.versions

import kotlinx.serialization.json.Json
import moe.crx.overport.utils.HttpUtil
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class VersionManager(val workingDirectory: File) {

    companion object {
        val json = Json { ignoreUnknownKeys = true }
        const val VERSION = "1.2.1"

        private fun isVersionLower(left: String, right: String): Boolean {
            val (lY, lM, lD) = left.substringBeforeLast('-').split('.').map { it.toInt() }
            val (rY, rM, rD) = right.substringBeforeLast('-').split('.').map { it.toInt() }

            return lY < rY || lM < rM || lD < rD || left < right
        }

        fun isIncompatible(release: OverportRelease): Boolean {
            return release.appRequired?.let { isVersionLower(VERSION, it) } ?: false
        }
    }

    private fun appendInstalled(version: OverportRelease) {
        val file = File(workingDirectory, "installed.json")

        val installed = (installed() + version).distinctBy { it.version }
        val text = json.encodeToString(ReleasesIndex(installed))

        file.writeText(text)
    }

    fun uninstall(version: String) {
        val file = File(workingDirectory, "installed.json")

        val installed = installed()
        val selected = versionSelector(version, installed, true) ?: return
        val processed = installed.filter { it.version != selected.version }
        val text = json.encodeToString(ReleasesIndex(processed))

        val librariesDir = workingDirectory.resolve("libraries/${selected.version}")
        if (librariesDir.isDirectory) {
            librariesDir.deleteRecursively()
        }

        file.writeText(text)
    }

    private fun versionSelector(
        version: String,
        releases: List<OverportRelease>,
        ignoreCompatibility: Boolean
    ): OverportRelease? {
        val check: (OverportRelease) -> Boolean = {
            !isIncompatible(it) || ignoreCompatibility
        }

        return when (version) {
            "experimental" -> {
                releases.firstOrNull { it.isExperimental && check(it) }
            }

            "latest" -> {
                releases.firstOrNull { check(it) }
            }

            else -> {
                releases.firstOrNull { it.version == version && check(it) }
            }
        }
    }

    fun checkout(version: String): String? {
        val availableEntry = versionSelector(version, available(), false)
        val installedEntry = availableEntry ?: versionSelector(version, installed(), false)

        if (installedEntry != null) {
            val librariesDir = workingDirectory.resolve("libraries/${installedEntry.version}")
            if (librariesDir.isDirectory) {
                return installedEntry.version
            }
        }

        val entry = availableEntry ?: return null
        val downloadUrl = entry.downloadUrl ?: return null
        val librariesDir = workingDirectory.resolve("libraries/${entry.version}")

        val downloadedArchive = runCatching {
            HttpUtil.download(downloadUrl)
        }.getOrElse {
            librariesDir.deleteRecursively()
            return null
        }

        librariesDir.mkdirs()

        ZipInputStream(downloadedArchive.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach {
                if (it.isDirectory) {
                    librariesDir.resolve(it.name).mkdirs()
                } else {
                    zip.copyTo(FileOutputStream(librariesDir.resolve(it.name)))
                }
            }
        }

        appendInstalled(entry)

        return entry.version
    }

    fun getLibraries(version: String): File {
        val entry = installed().firstOrNull { it.version == version }
        checkNotNull(entry)
        check(!isIncompatible(entry))

        return workingDirectory.resolve("libraries/${entry.version}")
    }

    fun installed(): List<OverportRelease> {
        val file = File(workingDirectory, "installed.json")

        runCatching {
            val resultBytes = file.readBytes()
            val index = json.decodeFromString<ReleasesIndex>(String(resultBytes))
            return index.releases.sortedWith(Comparator { o1, o2 ->
                if (isVersionLower(
                        o1.version,
                        o2.version
                    )
                ) 1 else -1
            })
        }

        return listOf()
    }

    fun available(): List<OverportRelease> {
        val url = "https://ovrp.crx.moe/api/v1/releases/index"

        runCatching {
            val resultBytes = HttpUtil.download(url)
            val index = json.decodeFromString<ReleasesIndex>(String(resultBytes))
            return index.releases.sortedWith(Comparator { o1, o2 ->
                if (isVersionLower(
                        o1.version,
                        o2.version
                    )
                ) 1 else -1
            })
        }

        return listOf()
    }
}
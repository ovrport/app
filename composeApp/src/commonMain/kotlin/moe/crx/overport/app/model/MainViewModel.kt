package moe.crx.overport.app.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import moe.crx.overport.app.decodeBitmap
import moe.crx.overport.patches.IconResizer
import moe.crx.overport.patches.PLATFORM_ICON_RESIZER
import moe.crx.overport.patching.PatcherContext
import moe.crx.overport.utils.HttpUtil.download
import moe.crx.overport.versions.VersionManager
import org.jetbrains.compose.resources.getString
import overportapp.composeapp.generated.resources.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

class MainViewModel(val dataDirectory: File, platformIconResizer: IconResizer) : ViewModel() {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        const val RELEASES_URL = "https://api.github.com/repos/ovrport/app/releases"
    }

    enum class ThemePreference {
        AUTO, LIGHT, DARK
    }

    init {
        PLATFORM_ICON_RESIZER = platformIconResizer
    }

    var themePreference by mutableStateOf(ThemePreference.AUTO)

    private var patcherState by mutableStateOf<PatcherContext?>(null)
    var versionManager by mutableStateOf(VersionManager(dataDirectory))
        private set
    var working by mutableStateOf(false)
        private set
    var currentProgress by mutableStateOf<String?>(null)
        private set
    var currentProgressFloat by mutableStateOf<Float?>(null)
        private set

    private suspend fun usePatcher(block: suspend PatcherContext.() -> Unit) {
        working = true
        withContext(Dispatchers.IO) {
            patcherState?.block()
        }
        working = false
        currentProgress = null
    }

    suspend fun checkout(overportVersion: String, fileName: String) {
        patcherState = PatcherContext(
            overportVersion,
            dataDirectory,
            fileName,
            ""
        )
        usePatcher {
            currentProgress = getString(Res.string.progress_preparing)
            currentProgressFloat = 0.0f
            checkout()
        }
    }

    suspend fun prepare(inputStream: InputStream) {
        usePatcher {
            currentProgress = getString(Res.string.progress_decompiling)
            currentProgressFloat = 0.25f
            prepare(inputStream)
        }
    }

    fun patchedName(): String {
        return patcherState?.outputFileName() ?: "output.apk"
    }

    suspend fun process(patches: Map<String, List<String>>) {
        usePatcher {
            currentProgress = getString(Res.string.progress_patching)
            currentProgressFloat = 0.50f
            patch(patches)
        }
    }

    suspend fun export(outputStream: OutputStream) {
        usePatcher {
            currentProgress = getString(Res.string.progress_compiling)
            currentProgressFloat = 0.75f
            export(outputStream)
        }
    }

    suspend fun cancel() {
        usePatcher {
            currentProgress = getString(Res.string.progress_cleaning)
            currentProgressFloat = 1.0f
            cleanup()
        }
        currentProgressFloat = null
        patcherState = null
    }

    fun currentAppName(): String? {
        return patcherState?.appName()
    }

    fun currentAppPackage(): String? {
        return patcherState?.appPackage()
    }

    fun currentAppVersion(): String? {
        return patcherState?.appVersionName()
    }

    fun currentAppIcon(): ImageBitmap? {
        return patcherState?.appIcon()?.let { file ->
            FileInputStream(file).use { stream ->
                stream.readBytes().decodeBitmap()
            }
        }
    }

    fun isApkLoaded(): Boolean {
        return patcherState?.isApkLoaded() ?: false
    }

    fun versionToUpdate(): GithubRelease? {
        return runCatching {
            json
                .decodeFromString<List<GithubRelease>>(String(download(RELEASES_URL)))
                .reduceOrNull { left, right -> if (left.publishedAt > right.publishedAt) left else right }
        }.getOrNull()
    }
}
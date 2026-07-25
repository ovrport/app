package moe.crx.overport.app.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.crx.overport.app.decodeBitmap
import moe.crx.overport.app.util.ModifierUtil.rounded
import moe.crx.overport.config.getApplicationInfo
import org.jetbrains.compose.resources.stringResource
import overportapp.composeapp.generated.resources.Res
import overportapp.composeapp.generated.resources.unknown_package
import java.util.*

val iconCache = mutableMapOf<String, ImageBitmap?>()
val coverCache = mutableMapOf<String, ImageBitmap?>()

@Composable
fun ApplicationInfo(
    applicationName: String,
    applicationPackage: String,
    applicationVersion: String,
    applicationIcon: ImageBitmap? = null,
    forceLocalInfo: Boolean = false,
    useCover: Boolean = false,
) {
    var iconIsLoading by remember(applicationPackage) { mutableStateOf(true) }
    var onlineIcon by remember(applicationPackage) { mutableStateOf(iconCache[applicationPackage]) }
    var onlineCover by remember(applicationPackage) { mutableStateOf(coverCache[applicationPackage]) }

    var onlineLabel by rememberSaveable(applicationPackage) { mutableStateOf<String?>(null) }

    LaunchedEffect(applicationPackage) {
        withContext(Dispatchers.IO) {
            if (iconCache.containsKey(applicationPackage)) {
                return@withContext
            }

            val info = getApplicationInfo(applicationPackage)
            onlineLabel = info?.displayName

            val icon = info?.run {
                val associated = images.associateBy { it.imageType }
                associated["APP_IMG_ICON"] ?: associated["APP_IMG_COVER_SQUARE"]
            }

            val cover = info?.run {
                val associated = images.associateBy { it.imageType }
                associated["APP_IMG_COVER_SQUARE"] ?: associated["APP_IMG_ICON"]
            }

            val iconBitmap = icon?.uri?.let { Base64.getDecoder().decode(it) }?.decodeBitmap()
            val coverBitmap = cover?.uri?.let { Base64.getDecoder().decode(it) }?.decodeBitmap()

            iconCache[applicationPackage] = iconBitmap
            coverCache[applicationPackage] = coverBitmap

            onlineIcon = iconBitmap
            onlineCover = coverBitmap

            iconIsLoading = false
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            FadeVisibility(!iconIsLoading) {
                AnimatedContent((onlineCover.takeIf { useCover } ?: onlineIcon)
                    .takeIf { !forceLocalInfo } ?: applicationIcon) {
                    if (it == null) {
                        Icon(
                            Icons.Default.Widgets,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Image(
                            it,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).rounded(8.dp)
                        )
                    }
                }
            }
            FadeVisibility(iconIsLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            }
        }
        Column(
            modifier = Modifier.height(48.dp),
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(onlineLabel.takeIf { !forceLocalInfo } ?: applicationName) {
                Text("$it ($applicationVersion)")
            }
            Text(
                applicationPackage ?: stringResource(Res.string.unknown_package),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

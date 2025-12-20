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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.crx.overport.app.decodeBitmap
import moe.crx.overport.app.util.ModifierUtil.rounded
import moe.crx.overport.config.getApplicationInfo
import org.jetbrains.compose.resources.stringResource
import overportapp.composeapp.generated.resources.Res
import overportapp.composeapp.generated.resources.unknown_package
import java.util.*

@Composable
fun ApplicationInfo(
    applicationName: String? = "Application",
    applicationPackage: String? = "com.company.application",
    applicationVersion: String? = "1.0.0",
    applicationIcon: ImageBitmap? = null,
    forceLocalInfo: Boolean = false,
) {
    var iconIsLoading by rememberSaveable(applicationPackage) { mutableStateOf(true) }
    var onlineIcon by rememberSaveable(applicationPackage) { mutableStateOf<ImageBitmap?>(null) }
    var onlineLabel by rememberSaveable(applicationPackage) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(applicationPackage) {
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val info = applicationPackage?.let { getApplicationInfo(it) }
                    onlineLabel = info?.displayName

                    val image = info?.run {
                        val associated = images.associateBy { it.imageType }
                        associated["APP_IMG_ICON"] ?: associated["APP_IMG_COVER_SQUARE"]
                    }

                    onlineIcon = image?.uri?.let { Base64.getDecoder().decode(it) }?.decodeBitmap()
                }
                iconIsLoading = false
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            FadeVisibility(!iconIsLoading) {
                AnimatedContent(onlineIcon.takeIf { !forceLocalInfo } ?: applicationIcon) { it ->
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

package moe.crx.overport.app.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import moe.crx.overport.app.model.GithubRelease
import moe.crx.overport.app.model.MainViewModel
import moe.crx.overport.app.theme.OverportTheme
import moe.crx.overport.versions.VersionManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import overportapp.composeapp.generated.resources.Res
import overportapp.composeapp.generated.resources.overport
import overportapp.composeapp.generated.resources.patcher_nav_item
import overportapp.composeapp.generated.resources.update_available
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    viewModel: MainViewModel,
    openFile: () -> Unit,
    saveFile: (String) -> Unit,
    openFlow: MutableSharedFlow<Pair<String, InputStream>?>,
    saveFlow: MutableSharedFlow<OutputStream?>,
) {
    OverportTheme(darkTheme = true) {
        val snackbarHostState = remember { SnackbarHostState() }

        val patcherNavItem =
            BottomNavItem(stringResource(Res.string.patcher_nav_item), Icons.Filled.Build, Icons.Outlined.Build) {
                PatcherScreen(viewModel, snackbarHostState, openFile, saveFile, openFlow, saveFlow)
            }

        var versionToUpdate by remember { mutableStateOf<GithubRelease?>(null) }
        val urlHandler = LocalUriHandler.current

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                versionToUpdate = viewModel.versionToUpdate()
            }
        }

        DynamicScaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(Res.drawable.overport),
                                contentDescription = null,
                                modifier = Modifier.size(112.dp, 24.dp)
                            )
                        }
                    },
                    actions = {
                        val isUpdateAvailable =
                            versionToUpdate?.name != null && versionToUpdate?.name != VersionManager.VERSION
                        FadeVisibility(isUpdateAvailable) {
                            FilledTonalButton(
                                modifier = Modifier.padding(8.dp, 0.dp),
                                onClick = { versionToUpdate?.htmlUrl?.let { urlHandler.openUri(it) } }
                            ) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(Res.string.update_available),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                )
            },
            navigationItems = listOf(patcherNavItem),
            itemsEnabled = !viewModel.working && !viewModel.isApkLoaded()
        )
    }
}
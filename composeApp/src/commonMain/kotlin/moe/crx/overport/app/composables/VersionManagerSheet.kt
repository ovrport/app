package moe.crx.overport.app.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.crx.overport.app.model.MainViewModel
import moe.crx.overport.versions.OverportRelease
import org.jetbrains.compose.resources.stringResource
import overportapp.composeapp.generated.resources.Res
import overportapp.composeapp.generated.resources.select_version_title
import overportapp.composeapp.generated.resources.you_have_no_versions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionManagerSheet(viewModel: MainViewModel, onSelected: (String) -> Unit, onCancel: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var available by remember { mutableStateOf<List<OverportRelease>?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            withContext(Dispatchers.IO) {
                available = viewModel.versionManager.available()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onCancel() },
    ) {
        if (available == null) {
            Row(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@ModalBottomSheet
        }

        val available = available ?: listOf()
        val installed = viewModel.versionManager.installed()
        val noMoreAvailable = installed.filter { rel -> available.firstOrNull { rel.version == it.version } == null }

        Text(
            text = stringResource(Res.string.select_version_title),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
        )

        if (available.isEmpty() && installed.isEmpty()) {
            Text(
                text = stringResource(Res.string.you_have_no_versions),
                modifier = Modifier.padding(16.dp, 0.dp),
                textAlign = TextAlign.Center,
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp, 0.dp),
        ) {
            itemsIndexed(available + noMoreAvailable) { index, release ->
                val isInstalled = installed.firstOrNull { it.version == release.version } != null
                VersionListItem(release, index, isInstalled) {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onSelected(release.version)
                    }
                }
            }
        }
    }
}
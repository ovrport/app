package moe.crx.overport.app.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import moe.crx.overport.app.util.PatchLocalizer.localizedPatch
import moe.crx.overport.patches.PATCH_REPLACE_ICON_LABEL
import moe.crx.overport.patching.Patch
import moe.crx.overport.patching.PatchStore
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import overportapp.composeapp.generated.resources.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Preview
fun ApplicationInfoContent(
    applicationName: String? = "Application",
    applicationPackage: String? = "",
    applicationVersion: String? = "1.0.0",
    applicationIcon: ImageBitmap? = null,
    onCancel: () -> Unit = {},
    onConfirm: (Map<String, List<String>>) -> Unit = {}
) {
    val enabledPatches = remember(applicationPackage) {
        mutableStateMapOf(
            *PatchStore.recommended().map { it.name to listOf<String>() }.toTypedArray()
        )
    }

    var patchIconArgument by remember(applicationPackage) {
        mutableStateOf("icon")
    }

    fun togglePatch(patch: Patch) {
        val checked = enabledPatches.containsKey(patch.name)

        if (checked) {
            enabledPatches.remove(patch.name)
        } else {
            enabledPatches[patch.name] = listOf()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ApplicationInfo(
                applicationName,
                applicationPackage,
                applicationVersion,
                applicationIcon,
                !enabledPatches.containsKey(PATCH_REPLACE_ICON_LABEL.name)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn {
                items(PatchStore.all()) {
                    ListItem(
                        headlineContent = {
                            Text(localizedPatch(it))
                        },
                        supportingContent = {
                            if (!it.isRecommended) {
                                Text(stringResource(Res.string.use_at_your_risk))
                            }
                        },
                        trailingContent = {
                            Checkbox(enabledPatches.containsKey(it.name), { _ ->
                                togglePatch(it)
                            })
                        },
                        modifier = Modifier.clickable {
                            togglePatch(it)
                        }
                    )
                    if (PATCH_REPLACE_ICON_LABEL == it) {
                        FadeVisibility(enabledPatches.containsKey(it.name)) {
                            ListItem(
                                headlineContent = {
                                    Text(stringResource(Res.string.replace_icon_with_icon))
                                },
                                trailingContent = {
                                    RadioButton(patchIconArgument == "icon", {
                                        patchIconArgument = "icon"
                                    })
                                },
                                modifier = Modifier.padding(24.dp, 0.dp, 0.dp, 0.dp).clickable {
                                    patchIconArgument = "icon"
                                }
                            )
                        }
                        FadeVisibility(enabledPatches.containsKey(it.name)) {
                            ListItem(
                                headlineContent = {
                                    Text(stringResource(Res.string.replace_icon_with_cover))
                                },
                                trailingContent = {
                                    RadioButton(patchIconArgument == "cover", {
                                        patchIconArgument = "cover"
                                    })
                                },
                                modifier = Modifier.padding(24.dp, 0.dp, 0.dp, 0.dp).clickable {
                                    patchIconArgument = "cover"
                                }
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onCancel
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (enabledPatches.containsKey(PATCH_REPLACE_ICON_LABEL.name)) {
                        enabledPatches[PATCH_REPLACE_ICON_LABEL.name] = listOf(patchIconArgument)
                    }
                    onConfirm(enabledPatches)
                }
            ) {
                Text(stringResource(Res.string.action_confirm))
            }
        }
    }
}
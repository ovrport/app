package moe.crx.overport.patches

import moe.crx.overport.patching.Patch
import moe.crx.overport.utils.nameAttribute
import moe.crx.overport.utils.named
import moe.crx.overport.utils.takeNodesEach

val PATCH_REMOVE_USES_LIBRARY = Patch("patch_remove_uses_library") {
    selectManifestJson {
        takeNodesEach({ named("manifest") }) {
            takeNodesEach({ named("application") }) {
                takeNodesEach({ named("uses-library") }) {
                    if (nameAttribute() != "libopenxr.google.so") null else this
                }
                takeNodesEach({ named("uses-native-library") }) {
                    if (nameAttribute() != "libopenxr.google.so") null else this
                }
            }
        }
    }
}
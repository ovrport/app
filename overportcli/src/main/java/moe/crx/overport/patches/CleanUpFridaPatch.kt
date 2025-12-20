package moe.crx.overport.patches

import moe.crx.overport.patching.Patch
import moe.crx.overport.utils.nameAttribute
import moe.crx.overport.utils.named
import moe.crx.overport.utils.takeNodesEach

val PATCH_CLEAN_UP_FRIDA = Patch("patch_clean_up_frida") {
    selectManifestJson {
        takeNodesEach({ named("manifest") }) {
            takeNodesEach({ named("application") }) {
                takeNodesEach({ named("activity") }) {
                    nameAttribute()?.let {
                        selectSmali(it.replace('.', '/')) {
                            replace(
                                "const-string (\\w+), \\\"frda\\\"\\s+?invoke-static \\{\\1\\}, Ljava/lang/System;->loadLibrary\\(Ljava/lang/String;\\)V",
                                ""
                            )
                        }
                    }

                    this
                }
            }
        }
    }
}
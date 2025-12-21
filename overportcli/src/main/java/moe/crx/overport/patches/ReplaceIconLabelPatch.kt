package moe.crx.overport.patches

import com.reandroid.json.JSONObject
import moe.crx.overport.config.getApplicationInfo
import moe.crx.overport.patching.Patch
import moe.crx.overport.utils.named
import moe.crx.overport.utils.takeAttributes
import moe.crx.overport.utils.takeAttributesEach
import moe.crx.overport.utils.takeNodesEach
import java.util.*

interface IconResizer {
    fun resize(bytes: ByteArray, width: Int, height: Int): ByteArray
}

fun createLabel(label: String): JSONObject {
    return JSONObject().put("name", "label").put("id", 16842753)
        .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android").put("value_type", "STRING")
        .put("data", label)
}

fun createIconReference(reference: Int): JSONObject {
    return JSONObject().put("name", "icon").put("id", 16842754)
        .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
        .put("value_type", "REFERENCE")
        .put("data", reference)
}

var PLATFORM_ICON_RESIZER: IconResizer? = null

val PATCH_REPLACE_ICON_LABEL = Patch("patch_replace_icon_label") { arguments ->
    var reference = 0
    var iconDownloaded = false
    var gameTitle: String? = null

    createFile("root/res/drawable/custom_icon_ovrp.png") {
        val info = getApplicationInfo(applicationPackage())

        gameTitle = info?.displayName

        val image = info?.run {
            val associated = images.associateBy { it.imageType }
            if (arguments.firstOrNull() == "cover") {
                associated["APP_IMG_COVER_SQUARE"] ?: associated["APP_IMG_ICON"]
            } else {
                associated["APP_IMG_ICON"] ?: associated["APP_IMG_COVER_SQUARE"]
            }
        }

        val bytes = image?.uri?.let { Base64.getDecoder().decode(it) }
        var resizedBytes = bytes?.let { PLATFORM_ICON_RESIZER?.resize(it, 512, 512) } ?: bytes

        if (resizedBytes != null) {
            iconDownloaded = runCatching { writeBytes(resizedBytes) }.isSuccess
        }
    }

    if (!iconDownloaded) {
        selectFile("root/res/drawable/custom_icon_ovrp.png") {
            delete()
        }
    }

    if (iconDownloaded) {
        selectPackageBlock {
            getOrCreate("", "drawable", "custom_icon_ovrp").apply {
                valueAsString = "res/drawable/custom_icon_ovrp.png"
                reference = resourceId
            }
        }

        selectManifestJson {
            takeNodesEach({ named("manifest") }) {
                takeNodesEach({ named("application") }) {
                    takeNodesEach({ named("activity") }) {
                        takeAttributesEach({ named("icon") }) {
                            null
                        }
                    }
                    takeNodesEach({ named("activity-alias") }) {
                        takeAttributesEach({ named("icon") }) {
                            null
                        }
                    }

                    takeAttributesEach({ named("icon") }) {
                        null
                    }

                    takeAttributes {
                        this?.put(createIconReference(reference))
                    }
                }
            }
        }
    }

    gameTitle?.let { gameTitle ->
        selectManifestJson {
            takeNodesEach({ named("manifest") }) {
                takeNodesEach({ named("application") }) {
                    takeNodesEach({ named("activity") }) {
                        takeAttributesEach({ named("label") }) {
                            null
                        }
                    }
                    takeNodesEach({ named("activity-alias") }) {
                        takeAttributesEach({ named("label") }) {
                            null
                        }
                    }

                    takeAttributesEach({ named("label") }) {
                        null
                    }

                    takeAttributes {
                        this?.put(createLabel(gameTitle))
                    }
                }
            }
        }
    }
}
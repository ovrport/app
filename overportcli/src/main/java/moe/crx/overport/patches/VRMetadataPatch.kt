package moe.crx.overport.patches

import com.reandroid.json.JSONArray
import com.reandroid.json.JSONObject
import moe.crx.overport.patching.Patch
import moe.crx.overport.utils.nameAttribute
import moe.crx.overport.utils.named
import moe.crx.overport.utils.takeNodes
import moe.crx.overport.utils.takeNodesEach

fun createMetadata(name: String, value: String): JSONObject {
    return JSONObject().put("node_type", "element").put("name", "meta-data").put(
        "attributes", JSONArray().put(
            JSONObject().put("name", "name").put("id", 16842755)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", name)
        ).put(
            JSONObject().put("name", "value").put("id", 16842788)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", value)
        )
    )
}

fun createUsesFeature(name: String): JSONObject {
    return JSONObject().put("node_type", "element").put("name", "uses-feature").put(
        "attributes", JSONArray().put(
            JSONObject().put("name", "name").put("id", 16842755)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", name)
        ).put(
            JSONObject().put("name", "required").put("id", 16843406)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "BOOLEAN").put("data", true)
        )
    )
}

fun createUsesPermission(name: String): JSONObject {
    return JSONObject().put("node_type", "element").put("name", "uses-permission").put(
        "attributes", JSONArray().put(
            JSONObject().put("name", "name").put("id", 16842755)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", name)
        )
    )
}

fun createProperty(key: String, value: String): JSONObject {
    return JSONObject().put("node_type", "element").put("name", "property").put(
        "attributes", JSONArray().put(
            JSONObject().put("name", "name").put("id", 16842755)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", key)
        ).put(
            JSONObject().put("name", "value").put("id", 16842788)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", value)
        )
    )
}

val PATCH_VR_METADATA = Patch("patch_vr_metadata") {
    selectManifestJson {
        takeNodesEach({ named("manifest") }) {
            takeNodesEach({ named("application") }) {
                takeNodesEach({ named("activity") }) {
                    takeNodes {
                        this?.put(
                            createProperty(
                                "android.window.PROPERTY_XR_ACTIVITY_START_MODE",
                                "XR_ACTIVITY_START_MODE_FULL_SPACE_UNMANAGED"
                            )
                        )
                    }
                }
                takeNodes {
                    this?.put(createUsesFeature("android.software.xr.api.openxr"))
                        ?.put(createUsesPermission("org.khronos.openxr.permission.OPENXR"))
                        ?.put(createUsesPermission("org.khronos.openxr.permission.OPENXR_SYSTEM"))
                }
                takeNodesEach({ named("meta-data") }) {
                    if (nameAttribute() == "com.oculus.supportedDevices") null else this
                }
                takeNodes {
                    this?.put(createMetadata("pvr.app.type", "vr"))
                        ?.put(createMetadata("com.yvr.intent.category.VR", "vr_only"))
                        ?.put(createMetadata("com.oculus.supportedDevices", "all"))
                }
            }
        }
    }
}
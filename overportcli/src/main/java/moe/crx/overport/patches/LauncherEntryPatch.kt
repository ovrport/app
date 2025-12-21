package moe.crx.overport.patches

import com.reandroid.json.JSONArray
import com.reandroid.json.JSONObject
import moe.crx.overport.patching.Patch
import moe.crx.overport.utils.*

fun createAction(value: String): JSONObject {
    return JSONObject().put("node_type", "element").put("name", "action").put(
        "attributes", JSONArray().put(
            JSONObject().put("name", "name").put("id", 16842755)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", value)
        )
    )
}

fun createCategory(value: String): JSONObject {
    return JSONObject().put("node_type", "element").put("name", "category").put(
        "attributes", JSONArray().put(
            JSONObject().put("name", "name").put("id", 16842755)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", value)
        )
    )
}

val PATCH_LAUNCHER_ENTRY = Patch("patch_launcher_entry") {
    selectManifestJson {
        takeNodesEach({ named("manifest") }) {
            takeNodesEach({ named("application") }) {
                takeNodesEach({ named("activity") }) {
                    takeNodesEach({ named("intent-filter") }) {
                        val hasIntent = elem<JSONArray>("nodes").elemEach<JSONObject> { named("category") }.map {
                            when (it.nameAttribute()) {
                                "com.oculus.intent.category.VR" -> true
                                "android.intent.category.LAUNCHER" -> true
                                else -> false
                            }
                        }.reduceOrNull { l, r -> l || r } ?: false

                        takeNodes {
                            if (!hasIntent) this else {
                                JSONArray().put(createAction("android.intent.action.MAIN"))
                                    .put(createCategory("android.intent.category.LAUNCHER"))
                                    .put(createCategory("com.oculus.intent.category.VR"))
                                    .put(createCategory("com.yvr.intent.category.VR"))
                                    .put(createCategory("org.khronos.openxr.intent.category.IMMERSIVE_HMD"))
                            }
                        }
                    }
                }
            }
        }
    }
}
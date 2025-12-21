package moe.crx.overport.patches

import moe.crx.overport.patching.Patch

val PATCH_REMOVE_VRAPI = Patch("patch_remove_vrapi", false) {
    selectWorkspace {
        file.resolve("root/lib").listFiles()?.forEach { archDirectory ->
            val arch = getLibraries().resolve("lib/${archDirectory.name}")

            if (arch.exists()) {
                val vrApi = archDirectory.listFiles().find { it.name.lowercase() == "libvrapi.so" }
                vrApi?.delete()
            }
        }
    }
}
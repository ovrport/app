package moe.crx.overport.patches

import moe.crx.overport.patching.Patch
import java.io.File

val PATCH_COPY_OVRPLUGIN_VRAPI = Patch("patch_copy_ovrplugin_vrapi") {
    selectWorkspace {
        file.resolve("root/lib").listFiles()?.forEach { archDirectory ->
            val arch = getLibraries().resolve("lib/${archDirectory.name}")

            if (arch.exists()) {
                val hasVrApi = archDirectory.listFiles().find { it.name.lowercase() == "libvrapi.so" }
                if (hasVrApi != null) {
                    arch.listFiles().filter { it.name == "libOVRPlugin.so" }.forEach { library ->
                        library.copyTo(File(archDirectory, library.name), true)
                    }
                    hasVrApi.delete()
                }
            }
        }
    }
}
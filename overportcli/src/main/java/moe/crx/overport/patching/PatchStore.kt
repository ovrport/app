package moe.crx.overport.patching

import moe.crx.overport.patches.*

object PatchStore {
    private val PATCHES = mutableMapOf<String, Patch>()

    private fun register(patch: Patch) {
        if (PATCHES[patch.name] != null) {
            throw IllegalStateException("Patch with name ${patch.name} is already registered.")
        }

        PATCHES[patch.name] = patch
    }

    fun all(): List<Patch> {
        return PATCHES.values.toList()
    }

    fun get(name: String): Patch? {
        return PATCHES[name]
    }

    fun recommended(): List<Patch> {
        return all().filter { it.isRecommended }
    }

    init {
        register(PATCH_COPY_LIBRARIES)
        register(PATCH_COPY_OVRPLUGIN_VRAPI)
        register(PATCH_REPLACE_ICON_LABEL)
        register(PATCH_FIX_MIN_ANDROID_SDK)
        register(PATCH_GENERATE_CONFIG)
        register(PATCH_REMOVE_LOCALIZED_NAMES)
        register(PATCH_CLEAN_UP_FRIDA)
        register(PATCH_OCULUS_UNITY)
        register(PATCH_OCULUS_UNREAL)
        register(PATCH_VR_METADATA)
        register(PATCH_LAUNCHER_ENTRY)
        register(PATCH_REMOVE_USES_LIBRARY)
        register(PATCH_FIX_UNREAL_CRASH)
        register(PATCH_META_XR_AUDIO)
        register(PATCH_MARK_AS_DEBUGGABLE)
        register(PATCH_MARK_ALLOW_BACKUP)
        register(PATCH_REMOVE_UNREAL_FORCE_QUIT)
        register(PATCH_FORCE_PASSTHROUGH)
        register(PATCH_DISABLE_SPACE_WARP)
        register(PATCH_DISABLE_CONTROLLER_OFFSET)
    }
}
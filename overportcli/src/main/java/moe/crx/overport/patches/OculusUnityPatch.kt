package moe.crx.overport.patches

import moe.crx.overport.patching.Patch

val PATCH_OCULUS_UNITY = Patch("patch_oculus_unity") {
    selectSmali("com/unity/oculus/OculusUnity") {
        replace(
            "(getIsOnOculusHardware\\(\\)Z.*?)return (\\w+)", "$1\nconst/4 $2, 0x1\nreturn $2"
        )
    }

    selectLibrary("libOculusXRPlugin.so") {
        replaceHex(
            "1F 1C 00 72 ?? ?? ?? ?? E9 07 9F 1A C0 00 80 52 20 00 A0 72 ?? ?? ?? ??",
            "1F 1C 00 72 ?? ?? ?? ?? 29 00 80 52 C0 00 80 52 20 00 A0 72 ?? ?? ?? ??"
        )
        replaceHex(
            "00 28 18 BF 01 20 ?? ?? 08 70 06 20 C0 F2 01 00",
            "01 20 00 BF 00 BF ?? ?? 08 70 06 20 C0 F2 01 00"
        )
    }
}
package moe.crx.overport.patches

import moe.crx.overport.patching.Patch

val PATCH_FIX_UNREAL_CRASH = Patch("patch_fix_unreal_crash") {
    selectSmali("com/epicgames/ue4/GameActivity") {
        replace(
            "sput-object (\\w+), Lcom\\/epicgames\\/ue4\\/GameActivity;->\\w+:Lcom\\/epicgames\\/ue4\\/GameActivity;",
            "$0\nsput-object $1, Lcom/unity3d/player/UnityPlayer;->currentActivity:Landroid/app/Activity;"
        )

        createSmali("com/unity3d/player/UnityPlayer") {
            if (readText().isBlank()) {
                appendLine(".class public Lcom/unity3d/player/UnityPlayer;")
                appendLine(".super Ljava/lang/Object;")
                appendLine(".field public static currentActivity:Landroid/app/Activity;")
            }
        }
    }
}
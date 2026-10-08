package app.jhc.patches.updatecheck

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption

internal object MainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "/MainActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)

internal object MusicMainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "/MusicActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)

internal object PhotosMainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "/HomeActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)

private val COMPAT = arrayOf(
    Compatibility(
        name = "Twitter",
        packageName = "com.twitter.android",
    ),
    Compatibility(
        name = "YouTube",
        packageName = "com.google.android.youtube",
    ),
    Compatibility(
        name = "YT Music",
        packageName = "com.google.android.apps.youtube.music",
    ),
    Compatibility(
        name = "Photos",
        packageName = "com.google.android.apps.photos",
    )
)
private const val EXTENSION_CLASS = "Lapp/morphe/extension/jhc/JhcUpdateCheckPatch;"

@Suppress("unused")
val JhcUpdateCheckPatchRes = resourcePatch(
    name = "j-hc Update Check Res",
    default = false
) {
    val currentTag by stringOption(
        key = "currentTag",
        title = "Current build tag",
        description = "Current build tag",
        required = true,
        default = "0"
    )
    val updateRepo by stringOption(
        key = "updateRepo",
        title = "Update repo",
        description = "Update repo",
        required = true,
        default = "j-hc/revanced-magisk-module"
    )
    compatibleWith(*COMPAT)

    execute {
        document("res/values/strings.xml").use { document ->
            document.documentElement.appendChild(
                document.createElement("string").apply {
                setAttribute("name", "jhc_current_tag")
                textContent = currentTag
            })
            document.documentElement.appendChild(
                document.createElement("string").apply {
                setAttribute("name", "jhc_update_repo")
                textContent = updateRepo
            })
        }
    }
}

@Suppress("unused")
val JhcUpdateCheckPatch = bytecodePatch(
    name = "j-hc Update Check",
    description = "j-hc/revanced-magisk-module update check",
    default = true
) {
    compatibleWith(*COMPAT)
    extendWith("extensions/jhc.mpe")
    execute {
        val mainActivityOnCreateFingerprint = when (packageMetadata.packageName) {
            "com.google.android.apps.youtube.music" -> MusicMainActivityOnCreateFingerprint
            "com.google.android.apps.photos" -> PhotosMainActivityOnCreateFingerprint
            else -> MainActivityOnCreateFingerprint
        }
        mainActivityOnCreateFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->checkUpdate(Landroid/content/Context;)V
            """
        )
    }
}

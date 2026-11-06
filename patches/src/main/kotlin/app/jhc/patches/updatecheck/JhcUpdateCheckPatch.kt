package app.jhc.patches.updatecheck

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import org.w3c.dom.Element

internal object MainActivityOnCreateFingerprint : Fingerprint(
    definingClass = "/MainActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)

private var launcherActivityName: String? = null

private val FindLauncherActivityPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { doc ->
            fun Element.names(tag: String) = getElementsByTagName(tag).let { l ->
                (0 until l.length).map { (l.item(it) as Element).getAttribute("android:name") }
            }

            val nodes = doc.getElementsByTagName("activity")
            for (i in 0 until nodes.length) {
                val activity = nodes.item(i) as Element
                val filters = activity.getElementsByTagName("intent-filter")
                val isLauncher = (0 until filters.length).any {
                    val f = filters.item(it) as Element
                    "android.intent.action.MAIN" in f.names("action") &&
                        "android.intent.category.LAUNCHER" in f.names("category")
                }
                if (isLauncher) {
                    launcherActivityName = activity.getAttribute("android:name")
                    return@use
                }
            }
        }
    }
}

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
    default = false
) {
    dependsOn(FindLauncherActivityPatch)
    extendWith("extensions/jhc.mpe")
    execute {
        val method = MainActivityOnCreateFingerprint.matchOrNull()?.method ?: run {
            var name = launcherActivityName
                ?: throw PatchException("Launcher activity not found in manifest")
            if (name.startsWith(".")) name = packageMetadata.packageName + name
            Fingerprint(
                definingClass = "L" + name.replace('.', '/') + ";",
                name = "onCreate",
                returnType = "V",
                parameters = listOf("Landroid/os/Bundle;")
            ).method
        }
        method.addInstructions(
            0,
            """
                invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->checkUpdate(Landroid/content/Context;)V
            """
        )
    }
}

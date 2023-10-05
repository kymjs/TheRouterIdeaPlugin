package cn.therouter.idea.about

import cn.therouter.idea.utils.getVersion
import cn.therouter.idea.utils.gotoUrl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import java.io.File
import java.util.regex.Pattern


class VersionAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val currentVersion = event.project?.basePath?.let { foundCurrentVersion(File(it)) } ?: "UnKnow"
        val version = getVersion(currentVersion)
        if (MessageDialogBuilder
                .okCancel(
                    "TheRouter",
                    "最新稳定版本为：${version.latestRelease} \n最新预览版为：${version.latestVersion}\n当前项目版本：$currentVersion \n\n${version.upgradeText}"
                )
                .noText("关闭")
                .yesText("版本日志")
                .icon(Messages.getInformationIcon())
                .ask(event.project)
        ) {
            gotoUrl("https://github.com/HuolalaTech/hll-wp-therouter-android/releases")
        }
    }
}

private val theRouterVersionStrSet = HashSet<String>()
private val versionSet = HashSet<String>()

fun foundCurrentVersion(projectFile: File): String {
    createIndex(projectFile) { root ->
        foundTheRouterVersionStr(
            root.readText()
                .replace(" ", "")
                .replace("\n", "")
        )
    }
    foundVersion(projectFile)

    if (versionSet.isEmpty()) {
        return "unknow"
    }
    var max = ""
    versionSet.forEach {
        if (max < it) {
            max = it
        }
    }
    return max
}

private fun createIndex(root: File, action: (File) -> Unit) {
    if (root.isFile) {
        if (root.name.endsWith(".gradle")
            || root.name.endsWith(".kts")
        ) {
            action(root)
        }
    } else {
        root.listFiles()?.forEach { file ->
            createIndex(file, action)
        }
    }
}

private fun foundTheRouterVersionStr(content: String) {
    val pattern = Pattern.compile("cn.therouter:router:[A-Za-z0-9_\\-\\.\\{\\}$]+[\"']")
    val m = pattern.matcher(content)
    while (m.find()) {
        val version = m.group()
            .replace("cn.therouter:router:", "")
            .replace("'", "")
            .replace("\"", "")
            .replace("\$", "")
            .replace("{", "")
            .replace("}", "")
        theRouterVersionStrSet.add(version)
    }
}

private fun foundVersion(projectFile: File) {
    val versionFieldSet = HashSet<String>()
    theRouterVersionStrSet.forEach { versionStr ->
        if (versionStr.contains("[0-9]\\.[0-9]\\.[0-9](-rc\\d)?".toRegex())) {
            versionSet.add(versionStr)
        } else {
            versionFieldSet.add(versionStr)
        }
    }
    createIndex(projectFile) { root ->
        versionFieldSet.forEach { field ->
            foundTheRouterVersionField(
                field,
                root.readText()
                    .replace(" ", "")
                    .replace("\n", "")
            )
        }
    }
}

private fun foundTheRouterVersionField(versionStr: String, content: String) {
    val pattern = Pattern.compile("$versionStr=[0-9]\\.[0-9]+\\.[0-9]+(-rc[0-9]+)?")
    val m = pattern.matcher(content)
    while (m.find()) {
        val version = m.group()
            .replace("$versionStr=", "")
        versionSet.add(version)
    }
}

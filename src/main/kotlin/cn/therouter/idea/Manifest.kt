package cn.therouter.idea

import cn.therouter.idea.utils.getVersion
import cn.therouter.idea.utils.gotoUrl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import java.io.File

const val PLUGIN_VERSION_NAME = "1.3.4"
const val PLUGIN_VERSION_CODE = 133
const val LATEST_LIBRARY_VERSION_NAME = "1.3.0-rc3"
const val RELEASE_LIBRARY_VERSION_NAME = "1.2.4"
const val LATEST_HARMONY_VERSION_NAME = "1.0.0"
const val RELEASE_HARMONY_VERSION_NAME = "1.0.0"

private const val NONE = -1
private const val HARMONY = 0
private const val ANDROID = 1

// 非零即真，负数初始值
private var os = NONE

fun isAndroid(project: Project?): Boolean {
    if (os == NONE) {
        initOS(project)
    }
    return os == ANDROID
}

fun initOS(project: Project?) {
    val version = getVersion()
    if (version.toolVersionCode > PLUGIN_VERSION_CODE) {
        if (MessageDialogBuilder.okCancel(
                "有新的 IDE 插件可更新",
                "请在 JetBrains 插件市场更新至" + version.toolVersionName
            )
                .noText("关闭")
                .yesText("查看")
                .icon(Messages.getInformationIcon())
                .ask(project)
        ) {
            gotoUrl("https://plugins.jetbrains.com/plugin/20047-therouter/")
        }
    }
    project?.basePath?.let {
        File(it).listFiles()?.forEach { file ->
            if (file.name == "hvigorfile.ts" || file.name == "oh-package.json5" || file.name == "oh-package.json") {
                os = HARMONY
                return
            }
        }
    }
    os = ANDROID
}
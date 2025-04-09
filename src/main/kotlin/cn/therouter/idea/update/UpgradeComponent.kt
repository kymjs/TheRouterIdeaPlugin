package cn.therouter.idea.update

import cn.therouter.idea.PLUGIN_VERSION_CODE
import cn.therouter.idea.initOS
import cn.therouter.idea.utils.getVersion
import cn.therouter.idea.utils.gotoUrl
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages

@Service(Service.Level.PROJECT)
class UpgradeComponent : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        initOS(project)
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
    }
}
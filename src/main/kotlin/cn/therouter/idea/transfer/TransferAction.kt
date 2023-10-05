package cn.therouter.idea.transfer

import cn.therouter.idea.PLUGIN_VERSION_CODE
import cn.therouter.idea.transfer.arouter.ARouterTransfer
import cn.therouter.idea.utils.getVersion
import cn.therouter.idea.utils.gotoUrl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.filechooser.FileSystemView

class TransferAction : AnAction() {

    private val routerNameList = HashMap<String, ITransfer>()

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val version = getVersion()
        if (version.toolVersionCode > PLUGIN_VERSION_CODE) {
            if (MessageDialogBuilder
                    .okCancel(
                        "TheRouter IDEA Plugin",
                        "当前插件版本过旧，可能造成迁移失败，请前往 AndroidStudio 插件市场更新 TheRouter 插件。"
                    )
                    .noText("取消")
                    .yesText("确定")
                    .icon(Messages.getInformationIcon())
                    .ask(project)
            ) {
                gotoUrl("https://therouter.cn/docs/2022/09/29/01")
            }
        } else {
            val projectPath = project.basePath ?: ""
            val latestVersion = version.latestVersion ?: ""
            initTransfer()
            val sdf = SimpleDateFormat()
            sdf.applyPattern("HHmmss")
            val fileName = "TheRouterTransfer-${sdf.format(Date())}.txt"
            val desktop = File(FileSystemView.getFileSystemView().homeDirectory, "Desktop")
            if (!desktop.exists()) {
                desktop.mkdir()
            }
            val file = File(desktop, fileName)
            if (MessageDialogBuilder
                    .okCancel(
                        "TheRouter 一键迁移工具",
                        "当前项目为：$projectPath\n\n即将迁移至 TheRouter $latestVersion。迁移完成后，会在桌面生成改动日志。请注意查看：\n\n${file.absolutePath}。"
                    )
                    .noText("取消")
                    .yesText("开始迁移")
                    .icon(Messages.getInformationIcon())
                    .ask(project)
            ) {
                routerNameList["ARouter"]?.transfer(projectPath, latestVersion, file)
            }
        }
    }

    private fun initTransfer() {
        routerNameList["ARouter"] = ARouterTransfer()
    }
}

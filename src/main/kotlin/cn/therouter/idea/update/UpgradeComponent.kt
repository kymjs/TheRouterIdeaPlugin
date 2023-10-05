package cn.therouter.idea.update

import cn.therouter.idea.PLUGIN_VERSION_CODE
import cn.therouter.idea.utils.getVersion
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class UpgradeComponent : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        super.projectOpened(project)
        val version = getVersion()

        if (version.toolVersionCode > PLUGIN_VERSION_CODE) {
            val notificationGroup = NotificationGroup(
                displayId = "TheRouter",
                displayType = NotificationDisplayType.BALLOON
            )

            notificationGroup.createNotification(
                title = "TheRouter：有新的 AS 插件可更新",
                content = "请在 AndroidStudio 插件市场更新至" + version.toolVersionName,
                type = NotificationType.INFORMATION
            ).notify(project)
        }
    }
}
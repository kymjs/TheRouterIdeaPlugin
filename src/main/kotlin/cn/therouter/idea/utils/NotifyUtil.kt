package cn.therouter.idea.utils

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import javax.swing.Icon

/**
 * 通知提示
 */
object NotifyUtil {

    @JvmStatic
    @JvmOverloads
    fun show(
        groupId: String = "router",
        type: NotificationType = NotificationType.INFORMATION,
        content: String,
        icon: Icon? = null
    ) {
        Notifications.Bus.notify(
            Notification(groupId, content, type).setIcon(icon)
        )
    }
}
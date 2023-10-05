package cn.therouter.idea.about

import cn.therouter.idea.utils.gotoUrl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class WXFeedbackAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val url = "https://therouter.cn/docs/2022/08/24/01"
        gotoUrl(url)
    }
}
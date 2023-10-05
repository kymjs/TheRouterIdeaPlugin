package cn.therouter.idea.about

import cn.therouter.idea.utils.gotoUrl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class GitHubFeedbackAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val url = "https://github.com/HuolalaTech/hll-wp-therouter-android/issues/new"
        gotoUrl(url)
    }
}
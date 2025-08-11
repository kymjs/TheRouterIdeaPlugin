package cn.therouter.idea.ai

import cn.therouter.idea.utils.RouteIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefBrowser
import javax.swing.Icon

class TheRouterAI : ToolWindowFactory {
    override val icon: Icon?
        get() = RouteIcons.logo

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val url = "https://ragflow.therouter.cn:8443/chat/share?shared_id=0f8edfe46eba11f0afb00242c0a89006&from=chat&auth=Q0Y2M1NWQ2NmNlYjExZjA5N2Y1MDI0Mm"
        val browser = JBCefBrowser(url)
//        browser.loadURL()
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(browser.component, "", false))
    }
}

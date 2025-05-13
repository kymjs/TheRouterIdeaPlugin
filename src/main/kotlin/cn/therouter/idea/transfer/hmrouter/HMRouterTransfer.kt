package cn.therouter.idea.transfer.hmrouter

import at.syntaxerror.json5.Json5Module
import cn.therouter.idea.transfer.ITransfer
import cn.therouter.idea.utils.getVersion
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File


class HMRouterTransfer : ITransfer {

    // key:file - value:fileContent
    private val etsFileContentMap = HashMap<File, String>()
    private val jsFileContentMap = HashMap<File, String>()
    private val json5FileContentMap = HashMap<File, String>()

    private val todoCustomChangeClass = HashSet<String>()

    override fun transfer(projectPath: String, version: String, logFile: File) {
        if (!logFile.exists()) {
            logFile.createNewFile()
        }

        val projectFile = File(projectPath)
        if (!projectFile.exists()) {
            logFile.appendText("转换失败\n找不到项目目录$projectPath，或插件无此目录的写权限\n")
            return
        }

        logFile.appendText("---------------------------------------------------------\n")
        logFile.appendText("开始生成遍历索引，请稍候...\n")
        createIndex(projectFile)
        logFile.appendText("---------------------------------------------------------\n")
        logFile.appendText("开始修改工程依赖文件 ...\n")
        handleHvigorFile()
        handleJson5File()
        logFile.appendText("---------------------------------------------------------\n")
        logFile.appendText("开始替换 .ets 代码...\n")
        handleETSFile()
        logFile.appendText("---------------------------------------------------------\n")
        if (todoCustomChangeClass.isEmpty()) {
            logFile.appendText("恭喜您，已全部转换完成。\n")
        } else {
            logFile.appendText("已部分转换完成，由于HMRouter实现不同，你还需要手动修改如下部分的文件。\n\n")
            todoCustomChangeClass.forEach {
                logFile.appendText("$it\n")
            }
        }
        logFile.appendText("---------------------------------------------------------\n")
        logFile.appendText("\n您还需要手动解决编译报错的部分（解决方式已经都给出在报错位置）\n")
        logFile.appendText("详细转换说明，请查阅官网文档：https://therouter.cn/docs/2022/09/05/01\n")

        val project = ProjectManager.getInstance().openProjects[0]
        val file = LocalFileSystem.getInstance().findFileByPath(logFile.absolutePath)
        if (file != null) {
            FileEditorManager.getInstance(project).openFile(file, true)
        }
    }

    /**
     * 获取索引信息
     */
    private fun createIndex(root: File) {
        if (root.isFile) {
            if (root.name.endsWith(".ets")) {
                etsFileContentMap[root] = root.readText()
            } else if (root.name == "hvigorfile.ts") {
                jsFileContentMap[root] = root.readText()
            } else if (root.name == "oh-package.json5" || root.name == "hvigor-config.json5") {
                json5FileContentMap[root] = root.readText()
            } else if (root.name == "hmrouter_config.json") {
                root.renameTo(File(root.parentFile, "therouter_build_config.json"))
            }
        } else {
            if (root.name != ".hvigor" && root.name != "oh_modules" && root.name != "build") {
                root.listFiles()?.forEach { file ->
                    createIndex(file)
                }
            }
        }
    }

    private fun handleHvigorFile() {
        jsFileContentMap.keys.forEach { file ->
            val text = jsFileContentMap[file]
                ?.replace("@hadss/hmrouter-plugin", "@therouter/plugin")
                ?: ""
            file.writeText(text)
        }
    }

    private val HMROUTER_LIBRARY = "@hadss/hmrouter"
    private val HMROUTER_PLUGIN = "@hadss/hmrouter-plugin"

    private fun handleJson5File() {
        fun replaceHMRouter(input: String): String {
            val pattern = """("$HMROUTER_LIBRARY"\s*:\s*")[^"]*(")""".toRegex()
            return pattern
                .replace(input, "$1${getVersion().latestHarmonyVersion}$2")
                .replace(HMROUTER_LIBRARY, "@therouter/library")
        }

        fun replaceHMRouterPlugin(input: String): String {
            val pattern = """("$HMROUTER_PLUGIN"\s*:\s*")[^"]*(")""".toRegex()
            return pattern
                .replace(input, "$1${getVersion().latestHarmonyVersion}$2")
                .replace(HMROUTER_PLUGIN, "@therouter/plugin")
        }
        json5FileContentMap.keys.forEach { file ->
            val text = json5FileContentMap[file] ?: ""
            if (text.contains(HMROUTER_PLUGIN)) {
                file.writeText(replaceHMRouterPlugin(text))
            } else if (text.contains(HMROUTER_LIBRARY)) {
                file.writeText(replaceHMRouter(text))
            }
        }
    }

    private fun handleETSFile() {
        fun replaceRouterInit(input: String): String {
            val pattern = Regex("HMRouterMgr\\.init\\(\\s*\\{[^}]*\\}\\)")
            return pattern.replace(input, "TheRouter.init(this.context)")
        }

        fun replaceRouterAnnotation(input: String): String {
            val pattern = """@HMRouter\(\s*\{\s*pageUrl\s*:\s*'([^'\s]*)'([^)]*)\}\s*\)""".toRegex()
            return pattern.replace(input) { matchResult ->
                val (pageUrl, rest) = matchResult.destructured
                // 检查是否有 singleton 属性
                val singletonPattern = """singleton\s*:\s*(true|false)""".toRegex()
                val singletonMatch = singletonPattern.find(rest)
                val singletonPart = if (singletonMatch != null) {
                    ", ${singletonMatch.value}"
                } else {
                    ""
                }
                "@Route({ path: '$pageUrl'$singletonPart })"
            }
        }

        fun replaceRouterAnnotation2(input: String): String {
            val pattern = """@HMRouter\(\s*\{\s*pageUrl\s*:\s*"([^"\s]*)"([^)]*)\}\s*\)""".toRegex()
            return pattern.replace(input) { matchResult ->
                val (pageUrl, rest) = matchResult.destructured
                // 检查是否有 singleton 属性
                val singletonPattern = """singleton\s*:\s*(true|false)""".toRegex()
                val singletonMatch = singletonPattern.find(rest)
                val singletonPart = if (singletonMatch != null) {
                    ", ${singletonMatch.value}"
                } else {
                    ""
                }
                "@Route({ path: '$pageUrl'$singletonPart })"
            }
        }

        fun transformPushString(input: String): String {
            val pattern1 = Regex("HMRouterMgr\\.push\\(\\s*\\{\\s*pageUrl\\s*:\\s*['\"]([^'\"\\s]*)['\"].*?\\}\\)")
            val pattern2 =
                Regex("HMRouterMgr\\.push\\(\\s*\\{\\s*pageUrl\\s*:\\s*['\"]([^'\"\\s]*)['\"].*?param\\s*:\\s*\\{(.*?)\\}[^)]*\\}\\)")
            val match2 = pattern2.find(input)
            if (match2 != null) {
                val (pageUrl, params) = match2.destructured
                return "TheRouter.build('$pageUrl').with({${params.trim()}}).navigation()"
            }
            val match1 = pattern1.find(input)
            if (match1 != null) {
                val (pageUrl) = match1.destructured
                return "TheRouter.build('$pageUrl').navigation()"
            }
            return input
        }

        fun transformReplaceString(input: String): String {
            val pattern1 = Regex("HMRouterMgr\\.replace\\(\\s*\\{\\s*pageUrl\\s*:\\s*['\"]([^'\"\\s]*)['\"].*?\\}\\)")
            val pattern2 =
                Regex("HMRouterMgr\\.replace\\(\\s*\\{\\s*pageUrl\\s*:\\s*['\"]([^'\"\\s]*)['\"].*?param\\s*:\\s*\\{(.*?)\\}[^)]*\\}\\)")
            val match2 = pattern2.find(input)
            if (match2 != null) {
                val (pageUrl, params) = match2.destructured
                return "TheRouter.build('$pageUrl').with({${params.trim()}}).replace()"
            }
            val match1 = pattern1.find(input)
            if (match1 != null) {
                val (pageUrl) = match1.destructured
                return "TheRouter.build('$pageUrl').replace()"
            }
            return input
        }
        etsFileContentMap.keys.forEach { file ->
            var text = replaceRouterInit(etsFileContentMap[file] ?: "")
            text = replaceRouterAnnotation(text)
            text = replaceRouterAnnotation2(text)
            text = transformPushString(text)
            text = transformReplaceString(text)
                .replace("HMNavigation", "TheRouterPage")
                .replace("@hadss/hmrouter", "@therouter/library")
                .replace("HMRouterMgr.pop(", "TheRouter.build().pop(")
                .replace("@HMService({ serviceName", "@Action({ action")
                .replace("HMRouterMgr.getCurrentParam", "TheRouter.getCurrentParam")
                .replace("HMRouterMgr.getService<", "TheRouter.get<")
                .replace("@HMServiceProvider", "\n这个类还要手动实现 IServiceProvider 接口\n@ServiceProvider")
                .replace("HMRouterMgr", "TheRouter") // 必须放到最后
                .replace("HMRouter", "Route") // 必须放到最后
                .replace("HMService", "Action") // 必须放到最后
            if (text.contains("@HMLifecycle")
                || text.contains("@HMInterceptor")
                || text.contains("@HMAnimator")
            ) {
                todoCustomChangeClass.add(file.absolutePath)
            }

            file.writeText(text)
        }
    }
}
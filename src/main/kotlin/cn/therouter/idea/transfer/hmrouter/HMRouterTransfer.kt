package cn.therouter.idea.transfer.hmrouter

import cn.therouter.idea.transfer.ITransfer
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
        logFile.appendText("开始修改 hvigorfile.ts ...\n")
        handleHvigorFile()
        logFile.appendText("---------------------------------------------------------\n")
        logFile.appendText("开始替换 .ets 代码...\n")
        handleETSFile()
        logFile.appendText("---------------------------------------------------------\n\n")
        if (todoCustomChangeClass.isEmpty()) {
            logFile.appendText("恭喜您，已全部转换完成。\n")
            logFile.appendText("您还需要手动判断with()方法的使用，建议全局搜索关键字：.with(  来查看使用位置有哪些。\n")
        } else {
            logFile.appendText("自动转换完成，您还需要做两件事：\n")
            logFile.appendText("1. 由于在如下类中，使用了自定义init()方法，需要手动修改对应逻辑至服务提供方初始化中。\n")
            todoCustomChangeClass.forEach {
                logFile.appendText("\t$it\n")
            }
            logFile.appendText("2. 您还需要手动判断 with()方法的使用，建议全局搜索关键字：.with(  来查看使用位置有哪些。\n")
        }
        logFile.appendText("详细转换说明，请查阅官网文档：https://therouter.cn/docs/2022/09/05/01\n")
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
            } else if (root.name == "oh-package.json5") {
                json5FileContentMap[root] = root.readText()
            } else if (root.name == "hmrouter_config.json") {
                root.renameTo(File(root.parentFile, "therouter_build_config.json"))
            }
        } else {
            root.listFiles()?.forEach { file ->
                createIndex(file)
            }
        }
    }

    private fun handleHvigorFile() {
        jsFileContentMap.keys.forEach { file ->
            val text = jsFileContentMap[file]
                ?.replace("@hadss/hmrouter-plugin", "@hll/therouter-plugin")
                ?: ""
            file.writeText(text)
        }
    }

    private fun handleETSFile() {
        fun replaceRouterInit(input: String): String {
            val pattern = Regex("HMRouterMgr\\.init\\(\\s*\\{[^}]*\\}\\)")
            return pattern.replace(input, "TheRouter.init(this.context)")
        }
        etsFileContentMap.keys.forEach { file ->
            val text = replaceRouterInit(etsFileContentMap[file] ?: "")
                .replace("HMNavigation", "TheRouterPage")
            file.writeText(text)
        }
    }
}
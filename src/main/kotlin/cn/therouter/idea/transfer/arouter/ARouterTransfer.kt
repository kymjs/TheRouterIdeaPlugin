package cn.therouter.idea.transfer.arouter

import cn.therouter.idea.transfer.ITransfer
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern


private const val AROUTER_PROVIDER = "com.alibaba.android.arouter.facade.template.IProvider"
private const val pattern = "@Route\\(path=\\S+\\)\\S*class"

private const val pattern1 =
    "ARouter\\s*\\.\\s*getInstance\\(\\s*\\)\\s*\\.\\s*navigation\\(.+\\s*::\\s*class\\.java\\)"
private const val pattern2 = "ARouter\\s*\\.\\s*getInstance\\(\\s*\\)\\s*\\.\\s*navigation\\(.+\\.class(\\.java)?\\)"
private const val pattern3 = "ARouter\\s*\\.\\s*getInstance\\(\\s*\\)\\s*\\.\\s*navigation\\(\\s*%s\\s*\\)"

class ARouterTransfer : ITransfer {

    // key:file - value:fileContent
    private val kotlinFileContentMap = HashMap<File, String>()
    private val javaFileContentMap = HashMap<File, String>()
    private val gradleFileContentMap = HashMap<File, String>()


    private val providerServiceSet = HashSet<ProviderClassNode>()
    private val providerTodoSet = HashSet<ProviderClassNode>()

    // key: routerPath - value: fullClassName
    private val routerPath2ClassName = HashMap<String, String>()

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
        val findIProviderLog = createProviderIndex()
        logFile.appendText(findIProviderLog)
        logFile.appendText("---------------------------------------------------------\n")
        logFile.appendText("开始修改ARouter中定义的IProvider代码...\n")
        val handleIProviderLog = handleProvider()
        logFile.appendText(handleIProviderLog)
        logFile.appendText("---------------------------------------------------------\n")
        logFile.appendText("开始替换其他代码...\n")
        doTransform(version)
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
            if (root.name.endsWith(".kt")) {
                kotlinFileContentMap[root] = root.readText()
            } else if (root.name.endsWith(".java")) {
                javaFileContentMap[root] = root.readText()
            } else if (root.name.endsWith(".gradle")) {
                gradleFileContentMap[root] = root.readText()
            }
        } else {
            root.listFiles()?.forEach { file ->
                createIndex(file)
            }
        }
    }

    /**
     * 遍历查找所有IProvider
     */
    private fun createProviderIndex(): String {
        providerTodoSet.add(ProviderClassNode(AROUTER_PROVIDER, null, null))
        val log = StringBuilder()
        while (providerTodoSet.isNotEmpty()) {
            for (node in ArrayList(providerTodoSet)) {
                val info1 = findProviderClass(javaFileContentMap, node, false)
                val info2 = findProviderClass(kotlinFileContentMap, node, true)
                providerTodoSet.remove(node)
                info1.addAll(info2)
                info1.sort()
                for (str in info1) {
                    if (str.isNotEmpty()) {
                        log.append(str).append("\n")
                    }
                }
            }
        }
        return log.toString()
    }

    private fun findProviderClass(
        map: HashMap<File, String>,
        parent: ProviderClassNode,
        isKotlin: Boolean
    ): ArrayList<String> {
        val info = ArrayList<String>()
        map.keys.forEach { file ->
            val codeContent = map[file]?.replace(Regex("\\s"), "") ?: ""
            val classSimpleName = file.name.substring(0, file.name.indexOf('.'))
            var classFullName = file.absolutePath.substring(0, file.absolutePath.lastIndexOf('.'))
            // 不在src下的不考虑
            if (classFullName.contains("/src/")) {
                classFullName = classFullName.substring(classFullName.indexOf("src/") + 4, classFullName.length)
                // 再过滤两级：src/main/java/
                classFullName = classFullName.substring(classFullName.indexOf('/') + 1, classFullName.length)
                classFullName = classFullName.substring(classFullName.indexOf('/') + 1, classFullName.length)
                classFullName = classFullName.replace("/".toRegex(), ".")
                val currentNode = ProviderClassNode(classFullName, file, parent)
                if (isKotlin) {
                    if (codeContent.contains("class" + classSimpleName + ":" + parent.fullName)
                        || codeContent.contains("class" + classSimpleName + ":" + parent.simpleName)
                        || codeContent.contains("class" + classFullName + ":" + parent.fullName)
                        || codeContent.contains("class" + classFullName + ":" + parent.simpleName)
                    ) {

                        // 排除抽象类的情况
                        if (isAbstractClass(codeContent, classSimpleName, classFullName)) {
                            providerTodoSet.add(currentNode)
                            info.add("发现IProvider抽象子类：$classFullName")
                        } else {
                            providerServiceSet.add(currentNode)
                            info.add("发现IProvider实现类：$classFullName")
                        }
                    } else if (codeContent.contains("interface" + classSimpleName + ":" + parent.fullName)
                        || codeContent.contains("interface" + classSimpleName + ":" + parent.simpleName)
                        || codeContent.contains("interface" + classFullName + ":" + parent.fullName)
                        || codeContent.contains("interface" + classFullName + ":" + parent.simpleName)
                    ) {
                        // 接口需要加入todo，继续向下找实现类
                        providerTodoSet.add(currentNode)
                        info.add("发现IProvider子接口：$classFullName")
                    }
                } else {
                    if (codeContent.contains("class" + classSimpleName + "implements" + parent.fullName)
                        || codeContent.contains("class" + classSimpleName + "implements" + parent.simpleName)
                        || codeContent.contains("class" + classFullName + "implements" + parent.fullName)
                        || codeContent.contains("class" + classFullName + "implements" + parent.simpleName)
                    ) {

                        // 排除抽象类的情况
                        if (isAbstractClass(codeContent, classSimpleName, classFullName)) {
                            info.add("发现IProvider抽象子类：$classFullName")
                            providerTodoSet.add(currentNode)
                        } else {
                            info.add("发现IProvider实现类：$classFullName")
                            providerServiceSet.add(currentNode)
                        }
                    } else if (codeContent.contains("interface" + classSimpleName + "extends" + parent.fullName)
                        || codeContent.contains("interface" + classSimpleName + "extends" + parent.simpleName)
                        || codeContent.contains("interface" + classFullName + "extends" + parent.fullName)
                        || codeContent.contains("interface" + classFullName + "extends" + parent.simpleName)
                    ) {
                        // 接口需要加入todo，继续向下找实现类
                        providerTodoSet.add(currentNode)
                        info.add("发现IProvider子接口：$classFullName")
                    }
                }
            }
        }
        return info
    }

    private fun isAbstractClass(
        contentCode: String,
        currentFileSimpleName: String,
        currentFileFullName: String
    ): Boolean {
        return (contentCode.contains("abstractclass$currentFileSimpleName") || contentCode.contains("abstractclass$currentFileFullName"))
    }

    private fun handleProvider(): String {
        val log = StringBuilder()
        val r = Pattern.compile(pattern)
        for (node in providerServiceSet) {
            val content = if (node.isKotlin) {
                kotlinFileContentMap[node.current]
            } else {
                javaFileContentMap[node.current]
            }?.replace(Regex("\\s"), "")
            content?.let {
                val m = r.matcher(it)
                if (m.find()) {
                    val group = m.group()
                    val start = group.indexOf('=') + 1
                    val end = group.indexOf(')')
                    if (start < 0 || end < 0 || end < start) {
                        log.append("${node.current}内解析异常：$group\n")
                    } else {
                        val routePath = group.substring(start, end)
                        routerPath2ClassName[routePath] = node.fullName

                        log.append("修改 ${node.simpleName} 类，添加 @ServiceProvider 方法\n")
                        if (node.isKotlin) {
                            val injectString =
                                "\n\n@com.therouter.inject.ServiceProvider \nfun theRouterServiceProvider(): ${node.parent?.fullName}=${node.simpleName}()"
                            val fileContent = (node.current?.readText() + injectString)
                                .replace(Regex("@Route\\s*\\(\\s*path\\s*=\\s*$routePath\\s*\\)"), "")
                                .replace(
                                    Regex("override\\s*fun\\s*init\\s*\\(\\s*\\S*\\s*:\\s*Context\\S*\\s*\\)\\s*\\{\\s*}"),
                                    ""
                                ).replace(
                                    Regex("override\\s*fun\\s*init\\s*\\(\\s*\\S*\\s*:\\s*Context\\S*\\s*\\)\\s*=\\s*Unit"),
                                    ""
                                )
                            if (fileContent.replace("\\s", "").contains("overridefuninit(")) {
                                log.append("检测到").append(node.fullName)
                                    .append(".init()中包含初始化逻辑，请手动处理!\n")
                                todoCustomChangeClass.add(node.fullName)
                            }
                            node.current?.writeText(fileContent)
                            kotlinFileContentMap[node.current!!] = fileContent
                        } else {
                            val injectString =
                                "\n\n\t@com.therouter.inject.ServiceProvider\n\tpublic static ${node.parent?.fullName} theRouterCreator(){ \n\t\treturn new ${node.simpleName}();\n\t}\n"
                            var fileContent = node.current?.readText()
                            val i = fileContent!!.lastIndexOf('}')
                            fileContent = (fileContent.substring(0, i) + injectString + fileContent.substring(i))
                                .replace(Regex("@Route\\s*\\(\\s*path\\s*=\\s*$routePath\\s*\\)"), "")
                                .replace(
                                    Regex("@Override\\s*public\\s*void\\s*init\\s*\\(\\s*\\S*Context\\s*\\S*\\s*\\)\\s*\\{\\s*}"),
                                    ""
                                ).replace(
                                    Regex("public\\s*void\\s*init\\s*\\(\\s*\\S*Context\\s*\\S*\\s*\\)\\s*\\{\\s*}"),
                                    ""
                                )
                            if (fileContent.replace("\\s", "").contains("publicvoidinit(")) {
                                log.append("检测到").append(node.fullName)
                                    .append(".init()中包含初始化逻辑，请手动处理!\n")
                                todoCustomChangeClass.add(node.fullName)
                            }
                            node.current?.writeText(fileContent)
                            javaFileContentMap[node.current!!] = fileContent
                        }
                    }
                }
            }
        }
        return log.toString()
    }

    private fun doTransform(theRoutertargetVersion: String) {
        gradleFileContentMap.keys.forEach { file ->
            handleGradle(file, gradleFileContentMap[file], theRoutertargetVersion)
        }
        for (file in javaFileContentMap.keys) {
            handleClass(false, file, javaFileContentMap[file], theRoutertargetVersion)
        }
        for (file in kotlinFileContentMap.keys) {
            handleClass(true, file, kotlinFileContentMap[file], theRoutertargetVersion)
        }
    }

    private fun handleGradle(file: File, content: String?, theRouterTargetVersion: String) {
        val text = content
            ?.replace("apply plugin: 'com.alibaba.arouter'", "apply plugin: 'therouter'")
            ?.replace("apply plugin: \"com.alibaba.arouter\"", "apply plugin: \"therouter\"")
            ?.replace(
                "com\\.alibaba:arouter-register:[0-9]\\.[0-9]\\.[0-9]".toRegex(),
                "cn.therouter:plugin:$theRouterTargetVersion"
            )
            ?.replace(
                "com\\.alibaba:arouter-api:[0-9]\\.[0-9]\\.[0-9]".toRegex(),
                "cn.therouter:router:$theRouterTargetVersion"
            )
            ?.replace(
                "com\\.alibaba:arouter-compiler:[0-9]\\.[0-9]\\.[0-9]".toRegex(),
                "cn.therouter:apt:$theRouterTargetVersion"
            )
            ?.replace(
                "kapt\\s*\\{\\s*arguments\\s*\\{\\s*arg\\s*\\(\\s*\"AROUTER_MODULE_NAME\"\\s*,\\s*project.getName\\(\\)\\s*;?\\s*\\)\\s*\\}\\s*\\}".toRegex(),
                ""
            )
            ?.replace(
                "apt\\s*\\{\\s*arguments\\s*\\{\\s*AROUTER_MODULE_NAME\\s*project.getName\\(\\)\\s*;\\s*\\}\\s*\\}".toRegex(),
                ""
            )
            ?.replace(
                "javaCompileOptions\\s*\\{\\s*annotationProcessorOptions\\s*\\{\\s*arguments\\s*=\\s*\\[\\s*AROUTER_MODULE_NAME\\s*:\\s*project.getName\\(\\)\\s*]\\s*\\}\\s*\\}".toRegex(),
                ""
            )
            ?.replace(
                "javaCompileOptions\\s*\\{\\s*annotationProcessorOptions\\s*\\{\\s*arguments\\s*=\\s*\\[\\s*AROUTER_MODULE_NAME\\s*:\\s*project.getName\\(\\)\\s*,\\s*AROUTER_GENERATE_DOC\\s*:\\s*\"enable\"\\s*\\]\\s*\\}\\s*\\}".toRegex(),
                ""
            ) ?: ""
        file.writeText(text)
    }


    private fun handleClass(isKotlin: Boolean, file: File, content: String?, theRoutertargetVersion: String) {
        if (file.absolutePath.contains("/buildSrc/")) {
            handleKotlinGradle(file, content, theRoutertargetVersion)
            return
        }
        var text = content
        text = handleProviderService(pattern1, text)
        text = handleProviderService(pattern2, text)
        text = handleProviderService3(isKotlin, text)

        // 必须放在最后
        text = text?.replace("import\\scom.alibaba.android.arouter.facade.template.IProvider;?".toRegex(), "")
            ?.replace("\\s*:\\s*IProvider\\s*\\{".toRegex(), " {")
            ?.replace("\\s*extends\\s*IProvider\\s*\\{".toRegex(), " {")
            ?.replace("\\s*implements\\s*IProvider\\s*\\{".toRegex(), " {")
            ?.replace("ARouter.getInstance().", "TheRouter.")
            ?.replace("ARouter.getInstance()", "TheRouter")
            ?.replace("com.alibaba.android.arouter.launcher.ARouter", "com.therouter.TheRouter")
            ?.replace("com.alibaba.android.arouter.facade.Postcard", "com.therouter.router.Navigator")
            ?.replace("com.alibaba.android.arouter.facade.annotation.Route", "com.therouter.router.Route")
            ?.replace("com.alibaba.android.arouter.facade.annotation.Autowired", "com.therouter.router.Autowired")
            ?.replace("Postcard", "Navigator")
            ?.replace(
                "com.alibaba.android.arouter.facade.callback.NavigationCallback",
                "com.therouter.router.interceptor.NavigationCallback"
            )
            ?.replace("ARouter.init", "// 可以直接删掉本行 ARouter.init")
            ?.replace("ARouter.openLog", "// 可以直接删掉本行 ARouter.openLog")
            ?.replace("ARouter.openDebug", "// 可以直接删掉本行 ARouter.openDebug")
        for (path in routerPath2ClassName.keys) {
            text = if (isKotlin) {
                text?.replace(path, routerPath2ClassName[path].toString() + "::class.java")
            } else {
                text?.replace(path, routerPath2ClassName[path].toString() + ".class")
            }
        }
        file.writeText(text ?: "")
    }

    private fun handleKotlinGradle(file: File, content: String?, theRoutertargetVersion: String) {
        val text = content?.replace(
            "com\\.alibaba:arouter-register:[0-9]\\.[0-9]\\.[0-9]".toRegex(),
            "cn.therouter:plugin:$theRoutertargetVersion"
        )?.replace(
            "com\\.alibaba:arouter-api:[0-9]\\.[0-9]\\.[0-9]".toRegex(),
            "cn.therouter:router:$theRoutertargetVersion"
        )?.replace(
            "com\\.alibaba:arouter-compiler:[0-9]\\.[0-9]\\.[0-9]".toRegex(),
            "cn.therouter:apt:$theRoutertargetVersion"
        ) ?: ""
        file.writeText(text)
    }

    private fun handleProviderService(pattern: String, content: String?): String? {
        content?.let {
            val r = Pattern.compile(pattern)
            val m: Matcher = r.matcher(content)
            if (m.find()) {
                val group = m.group()
                if (group.isNotEmpty()) {
                    val serviceClassStr = group.substring(group.lastIndexOf('(') + 1, group.length - 1)
                    val format = String.format("TheRouter.get(%s)", serviceClassStr)
                    return handleProviderService(pattern, content.replace(group, format))
                }
            }
        }
        return content
    }


    private fun handleProviderService3(isKotlin: Boolean, content: String?): String? {
        var text = content
        for (path in routerPath2ClassName.keys) {
            val format = if (isKotlin) {
                String.format("TheRouter.get(%s::class.java)", routerPath2ClassName[path])
            } else {
                String.format("TheRouter.get(%s.class)", routerPath2ClassName[path])
            }
            text = text?.replace(pattern3.toRegex(), format)
        }
        return text
    }

}
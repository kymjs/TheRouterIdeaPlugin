package cn.therouter.idea.navigator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

class LineMarkerUtils4 : LineMarkerFunction {
    private var allFile = ArrayList<VirtualFile>()

    private var allTheRouterPsi = HashMap<VirtualFile, HashSet<CodeWrapper>>()

    // filePath, allLineMarkerCode
    private var allFileLineMarker = HashMap<String, HashSet<CodeWrapper>>()

    override fun main(elements: MutableList<out PsiElement>): ArrayList<LineMarkerInfo<*>> {
        findAllTheRouterPsi(elements[0])
        createIndex(elements)
        return createLineMark(elements[0].containingFile.viewProvider.virtualFile)
    }

    private fun createIndex(elements: MutableList<out PsiElement>) {
        val virtualFilePath = elements[0].containingFile.viewProvider.virtualFile.canonicalPath
        virtualFilePath?.let { filePath ->
            var codeWrapperSet = allFileLineMarker[filePath]
            if (codeWrapperSet == null) {
                codeWrapperSet = HashSet()
            }
            elements.forEach { psiElement ->
                var codeWrapper = getRouteAnnotationCode(psiElement)
                if (codeWrapper != null) {
                    codeWrapperSet.add(codeWrapper)
                }
                codeWrapper = getTheRouterBuildCode(psiElement)
                if (codeWrapper != null) {
                    codeWrapperSet.add(codeWrapper)
                }
                codeWrapper = getActionInterceptorCode(psiElement)
                if (codeWrapper != null) {
                    codeWrapperSet.add(codeWrapper)
                }
            }
            allFileLineMarker[filePath] = codeWrapperSet
        }
    }


    private fun findAllTheRouterPsi(rootElement: PsiElement) {
        val scopes = GlobalSearchScope.projectScope(rootElement.project)
        val kotlinFiles = FilenameIndex.getAllFilesByExt(rootElement.project, "kt", scopes)
        val javaFiles = FilenameIndex.getAllFilesByExt(rootElement.project, "java", scopes)
        allFile.addAll(kotlinFiles)
        allFile.addAll(javaFiles)
        for (virtualFile in allFile) {
            PsiManager.getInstance(rootElement.project).findFile(virtualFile)?.let { psiFile ->
                val properties = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement::class.java)
                properties.forEach {
                    var codeWrapperSet = allTheRouterPsi[virtualFile]
                    if (codeWrapperSet == null) {
                        codeWrapperSet = HashSet()
                    }
                    var contains = false
                    for (item: CodeWrapper in codeWrapperSet) {
                        if (item.psiElement.text == it.text) {
                            item.psiElement = it
                            contains = true
                            break
                        }
                    }
                    if (!contains) {
                        var codeWrapper = getRouteAnnotationCode(it)
                        if (codeWrapper != null) {
                            codeWrapperSet.add(codeWrapper)
                        }
                        codeWrapper = getTheRouterBuildCode(it)
                        if (codeWrapper != null) {
                            codeWrapperSet.add(codeWrapper)
                        }
                        codeWrapper = getActionInterceptorCode(it)
                        if (codeWrapper != null) {
                            codeWrapperSet.add(codeWrapper)
                        }
                        allTheRouterPsi[virtualFile] = codeWrapperSet
                    }
                }
            }
        }
    }

    fun createLineMark(virtualFile: VirtualFile): ArrayList<LineMarkerInfo<*>> {
        val result = ArrayList<LineMarkerInfo<*>>()
        allFileLineMarker[virtualFile.canonicalPath]?.forEach { lineMarkerCode ->
            val targetSet = HashSet<PsiElement>()
            try {
                when (lineMarkerCode.type) {
                    TYPE_ROUTE_ANNOTATION -> {
                        getAllTheRouterBuildTargetPsi().forEach { targetPsi ->
                            if (isTheRouterBuild(targetPsi, lineMarkerCode.code)) {
                                targetSet.add(targetPsi)
                            }
                        }
                    }

                    TYPE_THEROUTER_BUILD -> {
                        getAllRouteAnnotationOrActionInteceptTargetPsi().forEach { targetPsi ->
                            if (isRouteAnnotation(targetPsi, lineMarkerCode.code)
                                || isTheRouterAddActionInterceptor(targetPsi, lineMarkerCode.code)
                            ) {
                                targetSet.add(targetPsi)
                            }
                        }
                    }

                    TYPE_ACTION_INTERCEPT -> {
                        getAllTheRouterBuildTargetPsi().forEach { targetPsi ->
                            if (isTheRouterBuild(targetPsi, lineMarkerCode.code)) {
                                targetSet.add(targetPsi)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
            try {
                if (targetSet.isNotEmpty()) {
                    val builder = NavigationGutterIconBuilder.create(getIcon(lineMarkerCode.type))
                    builder.setAlignment(GutterIconRenderer.Alignment.CENTER)
                    builder.setTargets(targetSet)
                    if (lineMarkerCode.type == TYPE_ROUTE_ANNOTATION || lineMarkerCode.type == TYPE_ACTION_INTERCEPT) {
                        builder.setTooltipTitle("TheRouter:跳转到使用处")
                    } else {
                        builder.setTooltipTitle("TheRouter:跳转到声明处")
                    }
                    result.add(builder.createLineMarkerInfo(lineMarkerCode.psiElement))
                } else {
                    val builder = NavigationGutterIconBuilder.create(getIcon(TYPE_NONE))
                    builder.setAlignment(GutterIconRenderer.Alignment.CENTER)
                    if (lineMarkerCode.type == TYPE_ROUTE_ANNOTATION || lineMarkerCode.type == TYPE_ACTION_INTERCEPT) {
                        builder.setTooltipTitle("未发现使用:TheRouter.build(${lineMarkerCode.code})")
                    } else {
                        val path = lineMarkerCode.code
                        if (!path.contains('"') && !path.isFirstUpper()) {
                            builder.setTooltipTitle("变量 ${lineMarkerCode.code} 内容未知，请开发者检查是否定义路由")
                        } else {
                            if (lineMarkerCode.psiElement.text.contains("action(")) {
                                builder.setTooltipTitle("未定义 ActionInterceptor(${lineMarkerCode.code})")
                            } else {
                                builder.setTooltipTitle("未声明 @Route(path=${lineMarkerCode.code})")
                            }
                        }
                    }
                    result.add(builder.createLineMarkerInfo(lineMarkerCode.psiElement))
                }
            } catch (_: Exception) {
            }
        }
        return result
    }

    private fun getAllTheRouterBuildTargetPsi(): ArrayList<PsiElement> {
        val result = ArrayList<PsiElement>()
        allTheRouterPsi.keys.forEach { file ->
            allTheRouterPsi[file]?.forEach { codeWrapper ->
                if (codeWrapper.type == TYPE_THEROUTER_BUILD) {
                    result.add(TargetPsiElement(codeWrapper.psiElement))
                }
            }
        }
        return result
    }

    private fun getAllRouteAnnotationOrActionInteceptTargetPsi(): ArrayList<PsiElement> {
        val result = ArrayList<PsiElement>()
        allTheRouterPsi.keys.forEach { file ->
            allTheRouterPsi[file]?.forEach { codeWrapper ->
                if (codeWrapper.type == TYPE_ROUTE_ANNOTATION || codeWrapper.type == TYPE_ACTION_INTERCEPT) {
                    result.add(TargetPsiElement(codeWrapper.psiElement))
                }
            }
        }
        return result
    }
}
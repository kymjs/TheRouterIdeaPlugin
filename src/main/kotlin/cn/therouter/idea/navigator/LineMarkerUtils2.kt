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

class LineMarkerUtils2 : LineMarkerFunction {
    private var allFile = HashSet<VirtualFile>()

    // filePath, allLineMarkerCode
    private var allFileLineMarker = HashMap<String, HashSet<CodeWrapper>>()

    override fun main(elements: MutableList<out PsiElement>): ArrayList<LineMarkerInfo<*>> {
        createIndex(elements)
        val virtualFilePath = elements[0].containingFile.viewProvider.virtualFile.canonicalPath
        return allFileLineMarker[virtualFilePath]?.let { findAllLineMarkInfo(elements[0], it) } ?: arrayListOf()
    }

    private fun createIndex(elements: MutableList<out PsiElement>) {
        val virtualFilePath = elements[0].containingFile.viewProvider.virtualFile.canonicalPath
        virtualFilePath?.let { filePath ->
            var codeWrapperSet = allFileLineMarker[filePath]
            if (codeWrapperSet == null) {
                codeWrapperSet = HashSet()
            }
            if (codeWrapperSet.isEmpty()) {
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
    }

    private fun findAllLineMarkInfo(
        rootElement: PsiElement,
        currentFileLineMarkSet: HashSet<CodeWrapper>
    ): ArrayList<LineMarkerInfo<*>> {
        val result = ArrayList<LineMarkerInfo<*>>()
        if (allFile.isEmpty()) {
            val scopes = GlobalSearchScope.projectScope(rootElement.project)
            val kotlinFiles = FilenameIndex.getAllFilesByExt(rootElement.project, "kt", scopes)
            val javaFiles = FilenameIndex.getAllFilesByExt(rootElement.project, "java", scopes)
            allFile.addAll(kotlinFiles)
            allFile.addAll(javaFiles)
        }
        currentFileLineMarkSet.forEach { lineMarkCode ->
            for (virtualFile in allFile) {
                PsiManager.getInstance(rootElement.project).findFile(virtualFile)?.let { psiFile ->
                    val properties = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement::class.java)
                    val targetSet = HashSet<TargetPsiElement>()
                    properties.forEach { targetPsi ->
                        // 根据当前content类型，查找需要跳转的目标代码
                        when (lineMarkCode.type) {
                            TYPE_ROUTE_ANNOTATION -> {
                                if (isTheRouterBuild(targetPsi, lineMarkCode.code)) {
                                    targetSet.add(TargetPsiElement(targetPsi))
                                }
                            }

                            TYPE_THEROUTER_BUILD -> {
                                if (isRouteAnnotation(targetPsi, lineMarkCode.code)) {
                                    targetSet.add(TargetPsiElement(targetPsi))
                                } else if (isTheRouterAddActionInterceptor(targetPsi, lineMarkCode.code)) {
                                    targetSet.add(TargetPsiElement(targetPsi))
                                }
                            }

                            TYPE_ACTION_INTERCEPT -> {
                                if (isTheRouterBuild(targetPsi, lineMarkCode.code)) {
                                    targetSet.add(TargetPsiElement(targetPsi))
                                }
                            }
                        }
                    }
                    createLineMark(lineMarkCode, targetSet)?.let { result.add(it) }
                }
            }
        }
        return result
    }

    private fun createLineMark(wrapper: CodeWrapper, set: HashSet<TargetPsiElement>): LineMarkerInfo<*>? = try {
        if (set.isNotEmpty()) {
            val builder = NavigationGutterIconBuilder.create(getIcon(wrapper.type))
            builder.setAlignment(GutterIconRenderer.Alignment.CENTER)
            builder.setTargets(set)
            if (wrapper.type == TYPE_ROUTE_ANNOTATION || wrapper.type == TYPE_ACTION_INTERCEPT) {
                builder.setTooltipTitle("TheRouter:跳转到使用处")
            } else {
                builder.setTooltipTitle("TheRouter:跳转到声明处")
            }
            builder.createLineMarkerInfo(wrapper.psiElement)
        } else {
            val builder = NavigationGutterIconBuilder.create(getIcon(TYPE_NONE))
            builder.setAlignment(GutterIconRenderer.Alignment.CENTER)
            if (wrapper.type == TYPE_ROUTE_ANNOTATION || wrapper.type == TYPE_ACTION_INTERCEPT) {
                builder.setTooltipTitle("未发现使用:TheRouter.build(${wrapper.code})")
            } else {
                val path = wrapper.code
                if (!path.contains('"') && !path.isFirstUpper()) {
                    builder.setTooltipTitle("变量 ${wrapper.code} 内容未知，请开发者检查是否定义路由")
                } else {
                    if (wrapper.psiElement.text.contains("action(")) {
                        builder.setTooltipTitle("未定义 ActionInterceptor(${wrapper.code})")
                    } else {
                        builder.setTooltipTitle("未声明 @Route(path=${wrapper.code})")
                    }
                }
            }
            builder.createLineMarkerInfo(wrapper.psiElement)
        }
    } catch (_: Exception) {
        null
    }
}
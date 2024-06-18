package cn.therouter.idea.navigator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

class LineMarkerUtils1 : LineMarkerFunction {

    // VirtualFilePath, All LineMarker Code
    private var allTheRouterPsi = HashMap<String, HashSet<CodeWrapper>>()

    // psiElement.hashCode(), LineMarker Info
    private val markerCache = HashMap<CodeWrapper, LineMarkerInfo<*>>()

    override fun main(elements: MutableList<out PsiElement>): Collection<LineMarkerInfo<*>> {
        findAllTheRouterPsi(elements[0])
        return elements[0].containingFile.viewProvider.virtualFile.canonicalPath?.let { createLineMark(it) }
            ?: arrayListOf()
    }

    private fun findAllTheRouterPsi(rootElement: PsiElement) {
        val scopes = GlobalSearchScope.projectScope(rootElement.project)
        val kotlinFiles = FilenameIndex.getAllFilesByExt(rootElement.project, "kt", scopes)
        val javaFiles = FilenameIndex.getAllFilesByExt(rootElement.project, "java", scopes)
        val allFile = ArrayList<VirtualFile>()
        allFile.addAll(kotlinFiles)
        allFile.addAll(javaFiles)

        val currentFile = rootElement.containingFile.viewProvider.virtualFile
        allFile.forEach { virtualFile ->
            if (virtualFile.canonicalPath == currentFile.canonicalPath) {
                updateTheRouterPsi(rootElement.project, virtualFile)
            } else {
                if (allTheRouterPsi[virtualFile.canonicalPath] == null) {
                    updateTheRouterPsi(rootElement.project, virtualFile)
                }
            }
        }
    }

    private fun updateTheRouterPsi(project: Project, virtualFile: VirtualFile) {
        val virtualFilePath = virtualFile.canonicalPath ?: ""
        PsiManager.getInstance(project).findFile(virtualFile)?.let { psiFile ->
            val properties = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement::class.java)
            properties.forEach {
                var codeWrapperSet = allTheRouterPsi[virtualFilePath]
                if (codeWrapperSet == null) {
                    codeWrapperSet = HashSet()
                }
                var contains = false
                for (item: CodeWrapper in codeWrapperSet) {
                    if (item.psiElement.getKey() == it.getKey()) {
                        item.psiElement = it
                        contains = true
                        break
                    }
                }
                if (!contains) {
                    val c = getRouteAnnotationCode(it) ?: getTheRouterBuildCode(it) ?: getActionInterceptorCode(it)
                    c?.let { codeWrapper ->
                        codeWrapperSet.add(codeWrapper)
                    }
                }
                allTheRouterPsi[virtualFilePath] = codeWrapperSet
            }
        }
    }

    private fun createLineMark(filePath: String): Collection<LineMarkerInfo<*>> {
        val result = HashSet<LineMarkerInfo<*>>()
        allTheRouterPsi[filePath]?.forEach { lineMarkerCode ->
            if (!markerCache.contains(lineMarkerCode)) {
                val targetSet = HashSet<TargetPsiElement>()
                try {
                    when (lineMarkerCode.type) {
                        TYPE_ROUTE_ANNOTATION -> {
                            getAllTheRouterBuildTargetPsi(lineMarkerCode.code).forEach { targetPsi ->
                                targetSet.add(targetPsi)
                            }
                        }

                        TYPE_THEROUTER_BUILD -> {
                            getAllRouteAnnotationOrActionInterceptTargetPsi(lineMarkerCode.code).forEach { targetPsi ->
                                targetSet.add(targetPsi)
                            }
                        }

                        TYPE_ACTION_INTERCEPT -> {
                            getAllTheRouterBuildTargetPsi(lineMarkerCode.code).forEach { targetPsi ->
                                targetSet.add(targetPsi)
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
                        val marker = builder.createLineMarkerInfo(lineMarkerCode.psiElement.toTargetPsi())
                        markerCache[lineMarkerCode] = marker
                        result.add(marker)
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
                                if (lineMarkerCode.psiElement.getKey().contains("action(")) {
                                    builder.setTooltipTitle("未定义 ActionInterceptor(${lineMarkerCode.code})")
                                } else {
                                    builder.setTooltipTitle("未声明 @Route(path=${lineMarkerCode.code})")
                                }
                            }
                        }
                        // 未定义的不加入 showStatusCache 缓存，直接加入结果集，下次重新获取
                        result.add(builder.createLineMarkerInfo(lineMarkerCode.psiElement.toTargetPsi()))
                    }
                } catch (_: Exception) {
                }
            } else {
                markerCache[lineMarkerCode]?.let { result.add(it) }
            }
        }
        return result
    }

    private fun getAllTheRouterBuildTargetPsi(code: String): ArrayList<TargetPsiElement> {
        val result = ArrayList<TargetPsiElement>()
        allTheRouterPsi.keys.forEach { path ->
            allTheRouterPsi[path]?.forEach { codeWrapper ->
                if (codeWrapper.type == TYPE_THEROUTER_BUILD && code == codeWrapper.code) {
                    result.add(TargetPsiElement(codeWrapper.psiElement))
                }
            }
        }
        return result
    }

    private fun getAllRouteAnnotationOrActionInterceptTargetPsi(code: String): ArrayList<TargetPsiElement> {
        val result = ArrayList<TargetPsiElement>()
        allTheRouterPsi.keys.forEach { path ->
            allTheRouterPsi[path]?.forEach { codeWrapper ->
                if ((codeWrapper.type == TYPE_ROUTE_ANNOTATION || codeWrapper.type == TYPE_ACTION_INTERCEPT)
                    && code == codeWrapper.code
                ) {
                    result.add(TargetPsiElement(codeWrapper.psiElement))
                }
            }
        }
        return result
    }
}
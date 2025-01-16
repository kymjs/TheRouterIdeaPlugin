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
import com.jetbrains.rd.generator.nova.PredefinedType

/**
 * 内存占用最低，每次都遍历文件psi，每1s返回一次结果
 */
class LineMarkerUtils2 : LineMarkerFunction {

    // VirtualFilePath, All LineMarker Code
    private val allTheRouterPsi = HashMap<String, HashSet<CodeWrapper>>()
    private val timeMap = HashMap<String, Long>()

    private var findAllFinish = false

    override fun main(elements: MutableList<out PsiElement>): Collection<LineMarkerInfo<*>> {
        val currentFile = elements[0].containingFile.viewProvider.virtualFile
        val currentPath = currentFile.canonicalPath ?: ""
        if (currentPath.isEmpty()) {
            return ArrayList()
        }
        // 1s内只计算一次
        if (System.currentTimeMillis() - (timeMap[currentPath] ?: 0L) < 1000L) {
            return ArrayList()
        }

        if (!findAllFinish) {
            findAllTheRouterPsi(elements[0])
        } else {
            updateTheRouterPsi(elements[0].project, currentFile)
        }

        val result = createLineMark(currentPath)
        timeMap[currentPath] = System.currentTimeMillis()
        return result
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
            // 如果是当前打开的文件，就更新缓存
            // 或者如果当前遍历的文件没有缓存标记，就添加缓存
            if (virtualFile.canonicalPath == currentFile.canonicalPath || allTheRouterPsi[virtualFile.canonicalPath] == null) {
                updateTheRouterPsi(rootElement.project, virtualFile)
            }
        }
        findAllFinish = true
    }

    /**
     * 缓存psi
     */
    private fun updateTheRouterPsi(project: Project, virtualFile: VirtualFile) {
        val virtualFilePath = virtualFile.canonicalPath ?: ""
        PsiManager.getInstance(project).findFile(virtualFile)?.let { psiFile ->
            allTheRouterPsi.remove(virtualFilePath)
            val codeWrapperSet = HashSet<CodeWrapper>()
            val psiCollection = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement::class.java)
            psiCollection.forEach {
                val c = getRouteAnnotationCode(it) ?: getTheRouterBuildCode(it) ?: getActionInterceptorCode(it)
                c?.let { codeWrapper ->
                    codeWrapperSet.add(codeWrapper)
                }
            }
            allTheRouterPsi[virtualFilePath] = codeWrapperSet
        }
    }

    private fun createLineMark(virtualFilePath: String): Collection<LineMarkerInfo<*>> {
        val result = HashSet<LineMarkerInfo<*>>()
        allTheRouterPsi[virtualFilePath]?.forEach { codeWrapper ->
            val targetSet = HashSet<TargetPsiElement>()
            try {
                when (codeWrapper.type) {
                    TYPE_ROUTE_ANNOTATION -> {
                        getAllTheRouterBuildTargetPsi(codeWrapper.code).forEach { targetPsi ->
                            targetSet.add(targetPsi)
                        }
                    }

                    TYPE_THEROUTER_BUILD -> {
                        getAllRouteAnnotationOrActionInterceptTargetPsi(codeWrapper.code).forEach { targetPsi ->
                            targetSet.add(targetPsi)
                        }
                    }

                    TYPE_ACTION_INTERCEPT -> {
                        getAllTheRouterBuildTargetPsi(codeWrapper.code).forEach { targetPsi ->
                            targetSet.add(targetPsi)
                        }
                    }
                }
            } catch (_: Exception) {
            }
            try {
                if (targetSet.isNotEmpty()) {
                    val builder = NavigationGutterIconBuilder.create(getIcon(codeWrapper.type))
                        .setAlignment(GutterIconRenderer.Alignment.CENTER)
                        .setTargets(targetSet)
                    if (codeWrapper.type == TYPE_ROUTE_ANNOTATION || codeWrapper.type == TYPE_ACTION_INTERCEPT) {
                        builder.setTooltipTitle("TheRouter:跳转到使用处")
                    } else {
                        builder.setTooltipTitle("TheRouter:跳转到声明处")
                    }
                    val marker = builder.createLineMarkerInfo(codeWrapper.psiElement.toTargetPsi())
                    result.add(marker)
                } else {
                    val builder = NavigationGutterIconBuilder.create(getIcon(TYPE_NONE))
                        .setAlignment(GutterIconRenderer.Alignment.CENTER)
                        .setTargets(targetSet)
                    if (codeWrapper.type == TYPE_ROUTE_ANNOTATION || codeWrapper.type == TYPE_ACTION_INTERCEPT) {
                        builder.setTooltipTitle("未发现使用:TheRouter.build(${codeWrapper.code})")
                    } else {
                        val path = codeWrapper.code
                        if (!path.contains('"') && !path.isFirstUpper()) {
                            builder.setTooltipTitle("变量 ${codeWrapper.code} 内容未知，请检查是否定义路由")
                        } else {
                            if (codeWrapper.psiElement.getKey().contains("action(")) {
                                builder.setTooltipTitle("未定义 ActionInterceptor(${codeWrapper.code})")
                            } else {
                                builder.setTooltipTitle("未声明 @Route(path=${codeWrapper.code})")
                            }
                        }
                    }
                    // 未定义的不加入 showStatusCache 缓存，直接加入结果集，下次重新获取
                    result.add(builder.createLineMarkerInfo(codeWrapper.psiElement.toTargetPsi()))
                }
            } catch (_: Exception) {
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
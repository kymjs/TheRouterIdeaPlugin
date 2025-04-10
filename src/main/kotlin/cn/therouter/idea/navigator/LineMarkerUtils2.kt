package cn.therouter.idea.navigator

import cn.therouter.idea.isAndroid
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

/**
 * 内存占用最低，只首次扫描工程，每5s返回一次结果
 */
class LineMarkerUtils2 : LineMarkerFunction {

    // VirtualFilePath, All LineMarker Code
    private val allTheRouterPsi = HashMap<String, HashSet<CodeWrapper>>()

    private var findAllFinish = false

    override fun main(elements: MutableList<out PsiElement>): Collection<LineMarkerInfo<*>> {
        val currentFile = elements[0].containingFile.viewProvider.virtualFile
        val currentPath = currentFile.canonicalPath ?: ""
        if (currentPath.isEmpty()) {
            return ArrayList()
        }

        if (!findAllFinish) {
            findAllTheRouterPsi(elements[0])
        } else {
            updateTheRouterPsi(elements[0].project, currentFile)
        }

        val result = createLineMark(currentPath)
        return result
    }

    private fun findAllTheRouterPsi(rootElement: PsiElement) {
        val scopes = GlobalSearchScope.projectScope(rootElement.project)
        val allFile = ArrayList<VirtualFile>()
        if (isAndroid()) {
            val kotlinFiles = FilenameIndex.getAllFilesByExt(rootElement.project, "kt", scopes)
            val javaFiles = FilenameIndex.getAllFilesByExt(rootElement.project, "java", scopes)
            allFile.addAll(kotlinFiles)
            allFile.addAll(javaFiles)
        } else {
            val etsFiles = FilenameIndex.getAllFilesByExt(rootElement.project, "ets", scopes)
            allFile.addAll(etsFiles)
        }
        allFile.forEach { virtualFile ->
            // 如果当前遍历的文件没有缓存标记，就添加缓存
            if (allTheRouterPsi[virtualFile.canonicalPath] == null) {
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
            val codeWrapperSet = allTheRouterPsi.remove(virtualFilePath) ?: HashSet()
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
            val targetList = ArrayList<TargetPsiElement>()
            try {
                when (codeWrapper.type) {
                    TYPE_ROUTE_ANNOTATION -> {
                        getAllTheRouterBuildTargetPsi(codeWrapper.code).forEach { targetPsi ->
                            targetList.add(targetPsi)
                        }
                    }

                    TYPE_THEROUTER_BUILD -> {
                        getAllRouteAnnotationOrActionInterceptTargetPsi(codeWrapper.code).forEach { targetPsi ->
                            targetList.add(targetPsi)
                        }
                    }

                    TYPE_ACTION_INTERCEPT -> {
                        getAllTheRouterBuildTargetPsi(codeWrapper.code).forEach { targetPsi ->
                            targetList.add(targetPsi)
                        }
                    }
                }
            } catch (_: Exception) {
            }
            try {
                // 排序后去重，只保留代码部分最长的psi
                targetList.sort()
                val newList = ArrayList<TargetPsiElement>()
                targetList.forEach {
                    if (!newList.contains(it)) {
                        newList.add(it)
                    }
                }
                targetList.clear()
                targetList.addAll(newList)

                if (targetList.isNotEmpty()) {
                    targetList.sort()
                    val builder = NavigationGutterIconBuilder.create(getIcon(codeWrapper.type))
                        .setAlignment(GutterIconRenderer.Alignment.CENTER)
                        .setTargets(targetList)
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
                        .setTargets(targetList)
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
                                builder.setTooltipTitle(if (isAndroid()) "未声明 @Route(path=${codeWrapper.code})" else "未声明 @Route({path:${codeWrapper.code}})")
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
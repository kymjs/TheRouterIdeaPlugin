package cn.therouter.idea.navigator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.isNullOrEmpty
import java.util.*

const val STATUS_NONE = 0
const val STATUS_ERROR = 1
const val STATUS_SHOWN = 2

class TheRouterLineMarker : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    // 行标记跳转的目标代码
    private val allTargetPsi = HashMap<String, HashSet<TargetPsiElement>>()

    // 需要加行标记的代码
    private val allMarkerPsi = HashMap<String, Pair<PsiElement, TargetContent>>()

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isNullOrEmpty()) {
            return
        }
        val filePath = elements[0].containingFile.viewProvider.virtualFile.canonicalPath

        elements.forEach { psiElement ->
            try {
                findCode(psiElement)?.let { targetContent ->
                    try {
                        val key = getKey(filePath, psiElement)
                        allMarkerPsi[key] = Pair(psiElement, targetContent)
                        val targetPsiSet = allTargetPsi[key] ?: HashSet()
                        findAllTargetPsi(elements[0].project, filePath, targetContent).forEach { newFind ->
                            var exists = false
                            targetPsiSet.forEach { target ->
                                if (newFind.text == target.text) {
                                    exists = true
                                }
                            }
                            if (!exists) {
                                targetPsiSet.add(newFind)
                            }
                        }
                        allTargetPsi[key] = targetPsiSet
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }

        val allMarkerStatus = HashMap<String, Int>()
        elements.forEach { element ->
            val key = getKey(filePath, element)
            allMarkerPsi[key]?.let { pair ->
                val psiElement = pair.first
                val targetContent = pair.second

                val all = allTargetPsi[key] ?: HashSet()
                if (all.isNotEmpty()) {
                    if (allMarkerStatus[key] != STATUS_SHOWN) {
                        allMarkerStatus[key] = STATUS_SHOWN
                        val builder = NavigationGutterIconBuilder.create(getIcon(targetContent.type))
                        builder.setAlignment(GutterIconRenderer.Alignment.CENTER)
                        builder.setTargets(all)
                        if (targetContent.type == TYPE_ROUTE_ANNOTATION || targetContent.type == TYPE_ACTION_INTERCEPT) {
                            builder.setTooltipTitle("TheRouter:跳转到使用处")
                        } else {
                            builder.setTooltipTitle("TheRouter:跳转到声明处")
                        }
                        result.add(builder.createLineMarkerInfo(psiElement))
                    }
                } else {
                    if (!allMarkerStatus.containsKey(key)) {
                        allMarkerStatus[key] = STATUS_ERROR
                        val builder = NavigationGutterIconBuilder.create(getIcon(TYPE_NONE))
                            .setAlignment(GutterIconRenderer.Alignment.CENTER)
                            .setTargets(listOf())

                        if (targetContent.type == TYPE_ROUTE_ANNOTATION || targetContent.type == TYPE_ACTION_INTERCEPT) {
                            builder.setTooltipTitle("未发现使用:TheRouter.build(${targetContent.code})")
                        } else {
                            val path = targetContent.code
                            if (!path.contains('"') && !path.isFirstUpper()) {
                                builder.setTooltipTitle("变量 ${targetContent.code} 内容未知，请开发者检查是否定义路由")
                            } else {
                                if (psiElement.text.contains("action(")) {
                                    builder.setTooltipTitle("未定义 ActionInterceptor(${targetContent.code})")
                                } else {
                                    builder.setTooltipTitle("未声明 @Route(path=${targetContent.code})")
                                }
                            }
                        }
                        result.add(builder.createLineMarkerInfo(psiElement))
                    }
                }
            }
        }
    }

    /**
     * 查找当前psi是否为TheRouter相关API
     */
    private fun findCode(psiElement: PsiElement): TargetContent? {
        var target = getRouteAnnotationCode(psiElement)
        if (target == null) {
            target = getActionInterceptorCode(psiElement)
        }
        if (target == null) {
            target = getNavigationCode(psiElement)
        }
        return target
    }

    private var fileSystemCache = HashSet<Cache>()

    /**
     * 查找入参 targetContent，能跳转到的目标代码
     */
    private fun findAllTargetPsi(
        project: Project,
        filePath: String?,
        content: TargetContent
    ): Collection<TargetPsiElement> {
        val result = HashSet<TargetPsiElement>()

        val runnable = Runnable {
            val scopes = GlobalSearchScope.projectScope(project)
            val kotlinFiles = FilenameIndex.getAllFilesByExt(project, "kt", scopes)
            val javaFiles = FilenameIndex.getAllFilesByExt(project, "java", scopes)
            val allCodeFiles = ArrayList(kotlinFiles)
            allCodeFiles.addAll(javaFiles)
            for (virtualFile in allCodeFiles) {
                if (virtualFile.canonicalPath == filePath) {
                    continue
                }
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                psiFile?.let {
                    val properties = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement::class.java)
                    properties.forEach {
                        fileSystemCache.add(Cache(it, psiFile.name))
                    }
                }
            }
        }

        runnable.run()
//        if (fileSystemCache.isEmpty()) {
//        } else {
//            Thread(runnable).start()
//        }

        fileSystemCache.forEach { cache ->
            // 根据当前content类型，查找需要跳转的目标代码
            when (content.type) {
                TYPE_ROUTE_ANNOTATION -> {
                    if (isTheRouterBuild(cache.psiElement, content.code)) {
                        result.add(TargetPsiElement(cache.psiElement, cache.className))
                        debug("findAllTargetPsi", "找到注解使用方：" + content.code)
                    }
                }

                TYPE_THEROUTER_BUILD -> {
                    if (isRouteAnnotation(cache.psiElement, content.code)) {
                        result.add(TargetPsiElement(cache.psiElement, cache.className))
                        debug("findAllTargetPsi", "找到path声明：" + content.code)
                    } else if (isTheRouterAddActionInterceptor(cache.psiElement, content.code)) {
                        result.add(TargetPsiElement(cache.psiElement, cache.className))
                        debug("findAllTargetPsi", "找到Action拦截：" + content.code)
                    }
                }

                TYPE_ACTION_INTERCEPT -> {
                    if (isTheRouterBuild(cache.psiElement, content.code)) {
                        result.add(TargetPsiElement(cache.psiElement, cache.className))
                        debug("findAllTargetPsi", "找到Action使用方：" + content.code)
                    }
                }
            }
        }
        return result
    }


    fun String.isFirstUpper(): Boolean {
        if (isEmpty()) return false
        val temp = substring(0, 1)
        return temp == temp.uppercase(Locale.getDefault())
    }

    private fun getKey(filePath: String?, psiElement: PsiElement) = filePath + TargetPsiElement(psiElement, "").getKey()
}

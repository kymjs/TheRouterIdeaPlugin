package cn.therouter.idea.navigator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
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
    private val allTargetPsi = HashMap<String, HashSet<TargetPsiElement>>()
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
                        val key = filePath + TargetPsiElement(psiElement, "").getKey()
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
                    } catch (e: Exception) {
                    }
                }
            } catch (e: Exception) {
            }
        }

        val allMarkerStatus = HashMap<String, Int>()
        elements.forEach { element ->
            val key = filePath + TargetPsiElement(element, "").getKey()
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
                        if (targetContent.type == TYPE_ANNOTATION || targetContent.type == TYPE_ACTION) {
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

                        if (targetContent.type == TYPE_ANNOTATION || targetContent.type == TYPE_ACTION) {
                            builder.setTooltipTitle("未发现使用:TheRouter.build(${targetContent.content})")
                        } else {
                            val path = targetContent.content
                            if (!path.contains('"') && !path.isFirstUpper()) {
                                builder.setTooltipTitle("变量 ${targetContent.content} 内容未知，请开发者检查是否定义路由")
                            } else {
                                builder.setTooltipTitle("未声明 @Route(path=${targetContent.content})")
                            }
                        }
                        result.add(builder.createLineMarkerInfo(psiElement))
                    }
                }
            }
        }
    }

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

    private fun findAllTargetPsi(
        project: Project,
        filePath: String?,
        target: TargetContent
    ): Collection<TargetPsiElement> {
        val allTargetPsi = HashSet<TargetPsiElement>()
        val scopes = GlobalSearchScope.projectScope(project)
        val kotlinFiles = FilenameIndex.getAllFilesByExt(project, "kt", scopes)
        val javaFiles = FilenameIndex.getAllFilesByExt(project, "java", scopes)
        val allCodeFiles = ArrayList(kotlinFiles)
        allCodeFiles.addAll(javaFiles)
        if (allCodeFiles.isNullOrEmpty()) {
            return HashSet()
        }
        for (virtualFile in allCodeFiles) {
            if (virtualFile.canonicalPath == filePath) {
                continue
            }
            val psiFile: PsiFile? = PsiManager.getInstance(project).findFile(virtualFile)
            psiFile ?: return HashSet()

            val properties = PsiTreeUtil.findChildrenOfType(psiFile, PsiElement::class.java)
            properties.forEach { psiElement ->

                when (target.type) {
                    TYPE_ANNOTATION -> {
                        if (isTheRouterBuild(psiElement, target.content)) {
                            allTargetPsi.add(TargetPsiElement(psiElement, psiFile.name))
                            debug("findAllTargetPsi", "找到注解使用方：" + target.content)
                        }
                    }

                    TYPE_NAVIGATION -> {
                        if (isRouteAnnotation(psiElement, target.content)) {
                            allTargetPsi.add(TargetPsiElement(psiElement, psiFile.name))
                            debug("findAllTargetPsi", "找到path声明：" + target.content)
                        }
                    }

                    TYPE_ACTION -> {
                        if (isTheRouterAddActionInterceptor(psiElement, target.content)) {
                            allTargetPsi.add(TargetPsiElement(psiElement, psiFile.name))
                            debug("findAllTargetPsi", "找到Action拦截：" + target.content)
                        }
                    }
                }
            }
        }
        return allTargetPsi
    }


    fun String.isFirstUpper(): Boolean {
        if (isEmpty()) return false
        val temp = substring(0, 1)
        return temp == temp.uppercase(Locale.getDefault())
    }
}

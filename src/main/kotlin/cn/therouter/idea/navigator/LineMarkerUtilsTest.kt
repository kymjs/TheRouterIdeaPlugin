package cn.therouter.idea.navigator

import cn.therouter.idea.utils.NotifyUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch

class LineMarkerUtilsTest : LineMarkerFunction {

    override fun main(elements: MutableList<out PsiElement>): ArrayList<LineMarkerInfo<*>> {
        val result = ArrayList<LineMarkerInfo<*>>()
        elements.forEach {
            process(it, result)
        }
        return result
    }

    private fun process(element: PsiElement, result: ArrayList<LineMarkerInfo<*>>) {
        // 检测Route注解
        if (element is PsiAnnotation) {
            processAnnotation(element, result)
        }
        // 检测方法TheRouter方法调用
        else if (element is PsiMethodCallExpression) {
            processMethodCall(element, result)
        }
    }

    private fun processAnnotation(element: PsiAnnotation, result: ArrayList<LineMarkerInfo<*>>) {
        // 获取注解的全限定名
        val qualifiedName = element.qualifiedName ?: return
        // 注解名称判断
        if (!"com.therouter.router.Route".equals(qualifiedName)) {
            return
        }
        // 解析属性
        val attributes: Array<out PsiNameValuePair> = element.parameterList.attributes
        for (attribute in attributes) {
            val key: String? = attribute.name
            // value为字符串或引用
            var value: String? = attribute.literalValue
            if (value == null && attribute.value is PsiReferenceExpression) {
                val ref = attribute.value as PsiReferenceExpression
                val resolve = ref.resolve()
                if (resolve is PsiVariable) {
                    value = resolve.initializer?.run {
                        StringUtil.unquoteString(text)
                    }
                }
            }
            if (value.isNullOrBlank()) {
                return
            }
            // 路径已匹配
            if ("path".equals(key, true)) {
                // 提示
                NotifyUtil.show(content = "${attribute.text}", icon = AllIcons.Ide.Rating)
                // 查找对应的PSI文件对象
                val proj = element.project
                val scope = GlobalSearchScope.projectScope(proj)
                // 查找跳转目标
                val targets = HashSet<PsiElement>()
                // 方法一: 获取所有Java文件 性能消耗比较大
                /*val psiManager = PsiManager.getInstance(proj)
                val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, scope) // FilenameIndex.getAllFilesByExt(proj, "java", scope)
                for (virtualFile in javaFiles) {
                    // 遍历文件找到调用TheRouter.build的参数
                    val psiFile = psiManager.findFile(virtualFile)
                    psiFile?.accept(object : JavaRecursiveElementVisitor() {
                        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                            // 检测方法调用
                            val expRef = expression.methodExpression
                            val expArgs = expression.argumentList
                            val expRefName = expRef.qualifiedName
                            if (expArgs.expressionCount != 1 || !expRefName.contains("TheRouter.")) {
                                return
                            }
                            // 检测路由path
                            val exp1 = expArgs.expressions[0]
                            var pathValue: String? = null
                            if (exp1 is PsiLiteralExpression) {
                                pathValue = StringUtil.unquoteString(exp1.text)
                            } else if (exp1 is PsiReferenceExpression) {
                                val resolve = exp1.resolve()
                                if (resolve is PsiVariable) {
                                    pathValue = resolve.initializer?.run {
                                        StringUtil.unquoteString(text)
                                    }
                                }
                            }
                            if (pathValue.isNullOrBlank() || pathValue != value) {
                                return
                            }
                            targets.add(expression.navigationElement)
                            result.add(
                                NavigationGutterIconBuilder.create(RouteIcons.icon_from)
                                    .setTargets(targets)
                                    .setAlignment(GutterIconRenderer.Alignment.CENTER)
                                    .setTooltipTitle(pathValue.orEmpty())
                                    .createLineMarkerInfo(element)
                            )
                        }
                    })
                }*/
                // 方法二：一般会把路由路径定义在静态变量里面，故只查找对应的引用
                if (attribute.value is PsiReferenceExpression) {
                    // 获取参数
                    val ref = attribute.value as PsiReferenceExpression
                    val param = StringUtil.unquoteString(ref.text)
                    if (param.isBlank()) {
                        return
                    }
                    // 获取引用
                    val refCalls = ReferencesSearch.search(ref, scope).findAll()
                    refCalls.forEach {
                        targets.add(it.element.navigationElement)
                    }
                    result.add(
                        NavigationGutterIconBuilder.create(getIcon(TYPE_ROUTE_ANNOTATION))
                            .setTargets(targets)
                            .setAlignment(GutterIconRenderer.Alignment.CENTER)
                            .setTooltipTitle(param)
                            .createLineMarkerInfo(element)
                    )
                }
                break
            }
        }
    }

    private fun processMethodCall(element: PsiMethodCallExpression, result: ArrayList<LineMarkerInfo<*>>) {
        // 检测方法调用
        val expRef = element.methodExpression
        val expArgs = element.argumentList
        val expRefName = expRef.qualifiedName
        if (expArgs.expressionCount != 1 || !expRefName.contains("TheRouter.")) {
            return
        }
        val targets = HashSet<PsiElement>()
        // 检测路由path、Service
        val exp = expArgs.expressions[0]
        val pathValue = StringUtil.unquoteString(exp.text)
        if (exp is PsiReferenceExpression) {

        } else if (exp is PsiClassObjectAccessExpression) {
            val operand = exp.operand
            targets.add(operand.navigationElement)
        }
        if (pathValue.isBlank()) {
            return
        }
        result.add(
            NavigationGutterIconBuilder.create(getIcon(TYPE_THEROUTER_BUILD))
                .setTargets(targets)
                .setAlignment(GutterIconRenderer.Alignment.CENTER)
                .setTooltipTitle(pathValue)
                .createLineMarkerInfo(element)
        )
    }
}
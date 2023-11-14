package cn.therouter.idea.navigator

import com.intellij.psi.PsiElement

/**
 * 返回路由注解相关代码
 */
fun getRouteAnnotationCode(element: PsiElement): TargetContent? {
    if (isRouteAnnotation(element)) {
        val content = element.text.replace(" ", "")
            .replace("\n", "")
            .replace("@Route(", "")
            .replace(")", "")

        val allParams = content.split(",")

        var path = ""
        allParams.forEach {
            if (it.startsWith("path=")) {
                path = it.substring("path=".length)
            }
        }
        if (path.isNotBlank()) {
            return TargetContent(TYPE_ROUTE_ANNOTATION, path)
        }
    }
    return null
}

/**
 * 返回ActionManager拦截器代码
 */
fun getActionInterceptorCode(psiElement: PsiElement): TargetContent? {
    if (isTheRouterAddActionInterceptor(psiElement)) {
        val path = matchActionInterceptor(psiElement.text)
        if (path.isNotBlank()) {
            return TargetContent(TYPE_ACTION_INTERCEPT, path)
        }
    }
    return null
}

/**
 * 返回 TheRouter.build(xxxx) 代码
 */
fun getNavigationCode(psiElement: PsiElement): TargetContent? {
    if (isTheRouterBuild(psiElement)) {
        val path = matchBuild(psiElement.text.replace(" ", ""))
        if (path.isNotBlank()) {
            return TargetContent(TYPE_THEROUTER_BUILD, path)
        }
    }
    return null
}

/**
 * 查找是否为TheRouter.build(xxxx)
 */
fun isTheRouterBuild(psiElement: PsiElement, path: String = ""): Boolean {
    val content = psiElement.text.replace(" ", "").replace("\n", "")
    val contains = if (path.isEmpty()) {
        content.startsWith("TheRouter.build(")
    } else {
        content.startsWith("TheRouter.build(") &&
                content.contains(Regex("TheRouter.build\\(\\S*${handlePath(path)}\\)"))
    }

    return contains && content.startsWith("TheRouter.build(")
            && content.endsWith(")")
            && (content.contains("navigation") ||
            content.contains("action") ||
            content.contains("createFragment") ||
            content.contains("createIntent") ||
            content.contains("createIntentWithCallback") ||
            content.contains("createFragmentWithCallback") ||
            content.contains("getUrlWithParams"))
}

fun isTheRouterAddActionInterceptor(psiElement: PsiElement, path: String = ""): Boolean {
    val content = psiElement.text.replace(" ", "").replace("\n", "")
    return if (path.isEmpty()) {
        content.startsWith("TheRouter.addActionInterceptor(") || content.startsWith("@ActionInterceptor(actionName=")
    } else {
        (content.startsWith("TheRouter.addActionInterceptor(")
                && content.contains(Regex("TheRouter.addActionInterceptor\\(\\S*${handlePath(path)},")))
                || (content.startsWith("@ActionInterceptor(actionName=")
                && content.contains(Regex("@ActionInterceptor\\(actionName=\\S*${handlePath(path)}")))
    }
}

fun isRouteAnnotation(psiElement: PsiElement, path: String = ""): Boolean {
    val content = psiElement.text.replace(" ", "").replace("\n", "")
    val containPath = if (path.isEmpty()) {
        content.contains("path=")
    } else {
        content.contains(Regex("path=\\S*${handlePath(path)},")) || content.contains(Regex("path=\\S*${handlePath(path)}\\)"))
    }

    if (containPath && content.startsWith("@Route(") && content.endsWith(")")) {
        val str = content.replaceFirst("@Route(", "")
        return !str.contains("@Route")
    }
    return false
}

fun handlePath(path: String): String {
    if (path.contains('.') && !path.contains('"')) {
        val index = path.lastIndexOf('.')
        if (index > 0 && index + 1 < path.length) {
            return path.substring(index + 1, path.length)
        }
    }
    return path
}
package cn.therouter.idea.navigator

import com.intellij.psi.PsiElement
import java.util.*

/**
 * 返回路由注解相关代码
 */
fun getRouteAnnotationCode(psiElement: PsiElement): CodeWrapper? {
    if (isRouteAnnotation(psiElement)) {
        val content = psiElement.getKey().replace("@Route(", "").replace(")", "")

        val allParams = content.split(",")

        var path = ""
        allParams.forEach {
            if (it.startsWith("path=")) {
                path = it.substring("path=".length)
            }
        }
        if (path.isNotBlank()) {
            return CodeWrapper(TYPE_ROUTE_ANNOTATION, handlePath(path), psiElement)
        }
    }
    return null
}

/**
 * 返回ActionManager拦截器代码
 */
fun getActionInterceptorCode(psiElement: PsiElement): CodeWrapper? {
    if (isTheRouterAddActionInterceptor(psiElement)) {
        val path = matchActionInterceptor(psiElement.getKey())
        if (path.isNotBlank()) {
            return CodeWrapper(TYPE_ACTION_INTERCEPT, handlePath(path), psiElement)
        }
    }
    return null
}

/**
 * 返回 TheRouter.build(xxxx) 代码
 */
fun getTheRouterBuildCode(psiElement: PsiElement): CodeWrapper? {
    if (isTheRouterBuild(psiElement)) {
        val path = matchBuild(psiElement.getKey())
        if (path.isNotBlank()) {
            return CodeWrapper(TYPE_THEROUTER_BUILD, handlePath(path), psiElement)
        }
    }
    return null
}

/**
 * 查找是否为TheRouter.build(xxxx)
 */
fun isTheRouterBuild(psiElement: PsiElement, path: String = ""): Boolean {
    val content = psiElement.getKey()
    val contains = if (path.isEmpty()) {
        content.startsWith("TheRouter.build(")
    } else {
        content.contains(Regex("TheRouter.build\\((\\S*\\.)*${handlePath(path)}\\)"))
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
    val content = psiElement.getKey()
    return if (path.isEmpty()) {
        content.startsWith("TheRouter.addActionInterceptor(") || content.startsWith("@ActionInterceptor(actionName=")
    } else {
        (content.startsWith("TheRouter.addActionInterceptor(")
                && content.contains(Regex("TheRouter.addActionInterceptor\\((\\S*\\.)*${handlePath(path)},")))
                || (content.startsWith("@ActionInterceptor(actionName=")
                && content.contains(Regex("@ActionInterceptor\\(actionName=(\\S*\\.)*${handlePath(path)}")))
    }
}

fun isRouteAnnotation(psiElement: PsiElement, path: String = ""): Boolean {
    val content = psiElement.getKey()
    val containPath = if (path.isEmpty()) {
        content.contains("path=")
    } else {
        content.contains(Regex("path=(\\S*\\.)*${handlePath(path)},"))
                || content.contains(Regex("path=(\\S*\\.)*${handlePath(path)}\\)"))
    }

    if (containPath && content.startsWith("@Route(") && content.endsWith(")")) {
        val str = content.replaceFirst("@Route(", "").replaceFirst(")", "")
        return !str.contains("@Route") && !str.contains(")")
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

fun String.isFirstUpper(): Boolean {
    if (isEmpty()) return false
    val temp = substring(0, 1)
    return temp == temp.uppercase(Locale.getDefault())
}
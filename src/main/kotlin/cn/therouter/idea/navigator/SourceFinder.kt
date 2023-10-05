package cn.therouter.idea.navigator

import com.intellij.psi.PsiElement

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
            return TargetContent(TYPE_ANNOTATION, path)
        }
    }
    return null
}

fun getActionInterceptorCode(psiElement: PsiElement): TargetContent? {
    if (psiElement.javaClass.name == "org.jetbrains.kotlin.psi.KtDotQualifiedExpression"
        || psiElement.javaClass.name == "com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl"
        || psiElement.javaClass.name == "com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl"
    ) {
        if (isTheRouterAddActionInterceptor(psiElement)) {
            val path = matchActionInterceptor(psiElement.text)
            if (path.isNotBlank()) {
                return TargetContent(TYPE_ACTION, path)
            }
        }
    }
    return null
}

fun getNavigationCode(psiElement: PsiElement): TargetContent? {
    if (isTheRouterBuild(psiElement)) {
        val path = matchBuild(psiElement.text.replace(" ", ""))
        if (path.isNotBlank()) {
            return TargetContent(TYPE_NAVIGATION, path)
        }
    }
    return null
}

fun isTheRouterBuild(psiElement: PsiElement, path: String = ""): Boolean {
    val content = psiElement.text.replace(" ", "").replace("\n", "")
    val contains = if (path.isEmpty()) {
        content.contains("TheRouter.build(")
    } else {
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
        content.contains("TheRouter.addActionInterceptor(")
    } else {
        content.contains(Regex("TheRouter.addActionInterceptor\\(\\S*${handlePath(path)},"))
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
package cn.therouter.idea.navigator

import com.intellij.psi.PsiElement

class Cache(val psiElement: PsiElement, val className: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cache

        if (psiElement != other.psiElement) return false
        if (className != other.className) return false

        return true
    }

    override fun hashCode(): Int {
        var result = psiElement.hashCode()
        result = 31 * result + className.hashCode()
        return result
    }
}

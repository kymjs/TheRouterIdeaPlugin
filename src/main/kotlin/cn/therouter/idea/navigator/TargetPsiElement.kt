package cn.therouter.idea.navigator

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException

open class TargetPsiElement(private val delegate: PsiElement) : PsiElement by delegate {

    fun getKey() = delegate.getKey()

    override fun getText(): String {
        val prefix:String = try {
            delegate.containingFile.name
        } catch (_:PsiInvalidElementAccessException) {
            ""
        }
        val text = delegate.getKey()
        val suffix = if (text.length > 80) {
            "${text.subSequence(0, 80)}..."
        } else {
            text
        }
        return "$prefix: $suffix"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TargetPsiElement

        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        val result = text.hashCode()
        return 31 * result
    }
}

fun PsiElement.getKey(): String {
    return text.replace(" ", "").replace("\n", "")
}

fun PsiElement.toTargetPsi() = if (this is TargetPsiElement) {
    this
} else {
    TargetPsiElement(this)
}

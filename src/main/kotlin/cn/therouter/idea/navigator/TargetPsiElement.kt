package cn.therouter.idea.navigator

import com.intellij.psi.PsiElement

open class TargetPsiElement(private val delegate: PsiElement, private val className: String) : PsiElement by delegate {

    fun getKey(): String {
        return delegate.text.replace(" ", "").replace("\n", "")
    }

    override fun getText(): String {
        val prefix = className
        val text = getKey()
        val suffix = if (text.length > 80) {
            "${text.subSequence(0, 80)}..."
        } else {
            text
        }
        return "$prefix: $suffix"
    }
}
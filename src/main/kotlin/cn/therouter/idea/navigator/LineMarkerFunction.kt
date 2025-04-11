package cn.therouter.idea.navigator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiElement

interface LineMarkerFunction {
    fun main(elements: MutableList<out PsiElement>): Collection<LineMarkerInfo<*>>
    fun create(element: PsiElement): LineMarkerInfo<*>?
}

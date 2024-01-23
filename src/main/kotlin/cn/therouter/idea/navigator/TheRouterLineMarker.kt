package cn.therouter.idea.navigator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.psi.PsiElement
import com.intellij.util.containers.isNullOrEmpty

private val utils = LineMarkerUtils3()

class TheRouterLineMarker : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
            elements: MutableList<out PsiElement>,
            result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty()) {
            return
        }

        val list = utils.main(elements)
        if (list.isNotEmpty()) {
            result.addAll(list)
        }
    }
}

package cn.therouter.idea.navigator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.psi.PsiElement

private val lineMarkerUtils = LineMarkerUtils()

class TheRouterLineMarker : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return lineMarkerUtils.create(element)
    }

//    private var function: LineMarkerFunction? = null
//    override fun collectSlowLineMarkers(
//        elements: MutableList<out PsiElement>,
//        result: MutableCollection<in LineMarkerInfo<*>>
//    ) {
//        if (elements.isEmpty()) {
//            return
//        }
//
//        if (function == null) {
//            val gradleProperties = File(elements[0].project.basePath, "gradle.properties")
//            if (gradleProperties.exists()) {
//                val properties = Properties()
//                properties.load(FileInputStream(gradleProperties))
//                val property = properties.getProperty("TheRouterIdeaPlugin")
//                function = when (property) {
//                    "off" -> null
//                    "2" -> lineMarkerUtils
//                    else -> lineMarkerUtils
//                }
//            } else {
//                function = lineMarkerUtils
//            }
//        }
//
//        val list = function?.main(elements)
//        if (list?.isNotEmpty() == true) {
//            result.addAll(list)
//        }
//    }
}

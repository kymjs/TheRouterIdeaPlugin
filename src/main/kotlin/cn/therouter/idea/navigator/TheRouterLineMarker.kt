package cn.therouter.idea.navigator

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.psi.PsiElement
import java.io.File
import java.io.FileInputStream
import java.util.Properties

private val utils1 = LineMarkerUtils1()
private val utils2 = LineMarkerUtils2()

class TheRouterLineMarker : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    private var function: LineMarkerFunction? = null
    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty()) {
            return
        }

        if (function == null) {
            val gradleProperties = File(elements[0].project.basePath, "gradle.properties")
            if (gradleProperties.exists()) {
                val properties = Properties()
                properties.load(FileInputStream(gradleProperties))
                val property = properties.getProperty("TheRouterPlugin")
                function = when (property) {
                    "off" -> null
                    "1" -> utils1
                    "2" -> utils2
                    else -> utils2
                }
            } else {
                function = utils2
            }
        }

        val list = function?.main(elements)
        if (list?.isNotEmpty() == true) {
            result.addAll(list)
        }
    }
}

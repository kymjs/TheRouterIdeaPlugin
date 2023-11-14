package cn.therouter.idea.navigator

import com.intellij.openapi.util.IconLoader
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.swing.Icon

fun matchBuild(srcStr: String): String {
    val reg = "build\\((.*?)\\)"
    val pattern = Pattern.compile(reg)
    val matcher = pattern.matcher(srcStr)
    return if (matcher.find()) {
        matcher.group(1)
    } else {
        ""
    }
}

fun matchActionInterceptor(srcStr: String): String {
    if (srcStr.contains("actionName=")) {
        val start = srcStr.indexOf("actionName=") + 11
        val end = srcStr.indexOf(")") - 1
        if (start < end) {
            return srcStr.substring(start, end)
        }
    }

    val reg = "addActionInterceptor\\((.*?),"
    val pattern = Pattern.compile(reg)
    val matcher = pattern.matcher(srcStr)
    return if (matcher.find()) {
        matcher.group(1)
    } else {
        ""
    }
}

fun getIcon(type: Int): Icon {
    return when (type) {
        TYPE_ROUTE_ANNOTATION -> IconLoader.getIcon("/icon/icon_from.png")
        TYPE_THEROUTER_BUILD -> IconLoader.getIcon("/icon/icon_to.png")
        TYPE_ACTION_INTERCEPT -> IconLoader.getIcon("/icon/icon_from.png")
        else -> IconLoader.getIcon("/icon/icon_warn.png")
    }
}

fun debug(tag: String, msg: String) {
    val file = File("/Users/kymjs/Desktop/therouter.log")
    if (file.exists()) {
        file.appendText(SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()) + "    $tag::$msg\n")
    }
}
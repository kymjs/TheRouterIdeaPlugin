package cn.therouter.idea.utils

import com.intellij.openapi.util.IconLoader.getIcon


object RouteIcons {
    @JvmField
    val icon_from = getIcon("/icons/icon_from.png", RouteIcons::class.java)
    @JvmField
    val icon_to = getIcon("/icons/icon_to.png", RouteIcons::class.java)
    @JvmField
    val icon_warn = getIcon("/icons/icon_warn.png", RouteIcons::class.java)
}
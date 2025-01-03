package cn.therouter.idea.utils

import com.intellij.openapi.util.IconLoader.getIcon


object RouteIcons {
    @JvmField
    val icon_from = getIcon("/icon/icon_from.png", RouteIcons::class.java)
    @JvmField
    val icon_to = getIcon("/icon/icon_to.png", RouteIcons::class.java)
    @JvmField
    val icon_warn = getIcon("/icon/icon_warn.png", RouteIcons::class.java)
}
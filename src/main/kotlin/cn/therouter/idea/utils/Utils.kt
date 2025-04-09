package cn.therouter.idea.utils

import cn.therouter.idea.*
import cn.therouter.idea.bean.VersionBean
import com.google.gson.Gson

fun gotoUrl(url: String) {
    try {
        val uri = java.net.URI.create(url)
        val desktop = java.awt.Desktop.getDesktop()
        if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
            desktop.browse(uri)
        }
    } catch (e: Exception) {
    }
}

fun getVersion(currentVersion: String = ""): VersionBean {
    val json = try {
        doGet("https://cdn.kymjs.com:8843/therouter/upgrade.json?currentVersion=$currentVersion")
    } catch (e: Exception) {
        "{\n" +
                "    \"toolVersionName\": \"$PLUGIN_VERSION_NAME\",\n" +
                "    \"toolVersionCode\": $PLUGIN_VERSION_CODE,\n" +
                "    \"latestRelease\": \"$RELEASE_LIBRARY_VERSION_NAME\",\n" +
                "    \"latestVersion\": \"$LATEST_LIBRARY_VERSION_NAME\",\n" +
                "    \"latestHarmonyRelease\": \"$RELEASE_HARMONY_VERSION_NAME\",\n" +
                "    \"latestHarmonyVersion\": \"$LATEST_HARMONY_VERSION_NAME\",\n" +
                "    \"upgradeText\": \"升级至最新版无API改动\",\n" +
                "    \"url\": \"https://therouter.cn/docs/2022/09/05/01\",\n" +
                "    \"aarVersionArray\": [\n" +
                "        \"$LATEST_LIBRARY_VERSION_NAME\"\n" +
                "    ],\n" +
                "    \"harVersionArray\": [\n" +
                "        \"$LATEST_HARMONY_VERSION_NAME\"\n" +
                "    ]\n" +
                "}"
    }
    return Gson().fromJson(json, VersionBean::class.java)
}
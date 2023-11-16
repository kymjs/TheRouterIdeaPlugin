package cn.therouter.idea

import cn.therouter.idea.navigator.handlePath

const val PLUGIN_VERSION_NAME = "1.2.3"
const val PLUGIN_VERSION_CODE = 123
const val LATEST_LIBRARY_VERSION_NAME = "1.2.0-rc5"
const val RELEASE_LIBRARY_VERSION_NAME = "1.1.4"


fun main() {
    val content = "@Route(path=HOME)"
    val path = "HOME"

    val result = content.contains(Regex("path=(\\S*\\.)*${handlePath(path)},")) || content.contains(
        Regex(
            "path=(\\S*\\.)*${
                handlePath(
                    path
                )
            }\\)"
        )
    )

    println(result)
}
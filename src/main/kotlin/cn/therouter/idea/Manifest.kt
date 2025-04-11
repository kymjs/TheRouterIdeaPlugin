package cn.therouter.idea

import com.intellij.openapi.project.Project
import java.io.File

const val PLUGIN_VERSION_NAME = "1.3.3"
const val PLUGIN_VERSION_CODE = 133
const val LATEST_LIBRARY_VERSION_NAME = "1.2.4-rc13"
const val RELEASE_LIBRARY_VERSION_NAME = "1.2.3"
const val LATEST_HARMONY_VERSION_NAME = "1.0.0-rc7"
const val RELEASE_HARMONY_VERSION_NAME = "1.0.0"

private const val NONE = -1
private const val HARMONY = 0
private const val ANDROID = 1

// 非零即真，负数初始值
private var os = NONE

fun isAndroid(): Boolean {
    return os == ANDROID
}

fun initOS(project: Project) {
    project.basePath?.let {
        File(it).listFiles()?.forEach { file ->
            if (file.name == "hvigorfile.ts" || file.name == "oh-package.json5" || file.name == "oh-package.json") {
                os = HARMONY
                return
            }
        }
    }
    os = ANDROID
}
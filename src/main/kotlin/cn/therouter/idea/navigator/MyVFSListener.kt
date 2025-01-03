package cn.therouter.idea.navigator

import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

/**
 * 监听VirtualFile文件变化
 */
open class MyVFSListener : BulkFileListener {
    override fun before(events: MutableList<out VFileEvent>) {

    }

    override fun after(events: MutableList<out VFileEvent>) {
        for (event in events) {
            if (event.isFromRefresh || (isCode(event) && event !is VFileContentChangeEvent)) {
                // update allFiles
                isChanged = true
                break
            }
        }
    }

    private fun isCode(event: VFileEvent): Boolean {
        return event.path.endsWith("java") || event.path.endsWith("kt")
    }

    companion object {
        @JvmStatic
        var isChanged = false
    }
}
package cn.therouter.idea.transfer

import java.io.File

interface ITransfer {
    fun transfer(projectPath: String, version: String, logFile: File)
}
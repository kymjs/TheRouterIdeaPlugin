package cn.therouter.idea.transfer.arouter

import java.io.File
import java.util.*

class ProviderClassNode(var fullName: String, file: File?, val parent: ProviderClassNode?) {
    var simpleName: String = fullName.substring(fullName.lastIndexOf('.') + 1, fullName.length)

    @JvmField
    var current: File?

    init {
        current = file
    }

    val isKotlin: Boolean
        get() = if (current == null) {
            false
        } else current!!.name.endsWith(".kt")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProviderClassNode) return false
        return fullName == other.fullName && this.parent == other.parent
    }

    override fun hashCode(): Int {
        return Objects.hash(fullName, simpleName, parent)
    }
}
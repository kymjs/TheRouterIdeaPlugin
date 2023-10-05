package cn.therouter.idea.transfer.arouter

import java.util.*

class NodeRoute(var path: String, var returnType: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeRoute) return false
        return path == other.path && returnType == other.returnType
    }

    override fun hashCode(): Int {
        return Objects.hash(path, returnType)
    }
}
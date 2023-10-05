package cn.therouter.idea.transfer.arouter

import java.util.*

class Node(var current: String, var parent: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false
        return current == other.current && parent == other.parent
    }

    override fun hashCode(): Int {
        return Objects.hash(current, parent)
    }
}
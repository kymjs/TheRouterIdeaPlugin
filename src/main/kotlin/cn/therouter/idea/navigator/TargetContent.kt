package cn.therouter.idea.navigator


const val TYPE_NONE = 0
const val TYPE_ANNOTATION = 1
const val TYPE_NAVIGATION = 2
const val TYPE_ACTION = 3

class TargetContent(
    var type: Int = TYPE_NONE,
    var content: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TargetContent
        if (type != other.type) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + content.hashCode()
        return result
    }

    override fun toString(): String {
        return "TargetContent(type=$type, content='$content')"
    }
}

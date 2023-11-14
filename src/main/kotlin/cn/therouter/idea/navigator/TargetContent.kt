package cn.therouter.idea.navigator


const val TYPE_NONE = 0
const val TYPE_ROUTE_ANNOTATION = 1  // @Route(path=xxx)
const val TYPE_THEROUTER_BUILD = 2  // TheRouter.build(xxx)
const val TYPE_ACTION_INTERCEPT = 3  // TheRouter.addActionInterceptor(xxx) || @ActionInterceptor(actionName=xxx)

class TargetContent(
    var type: Int = TYPE_NONE,
    var code: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TargetContent
        if (type != other.type) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + code.hashCode()
        return result
    }

    override fun toString(): String {
        return "TargetContent(type=$type, content='$code')"
    }
}

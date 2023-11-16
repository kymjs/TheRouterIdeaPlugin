package cn.therouter.idea.navigator

import com.intellij.psi.PsiElement


const val TYPE_NONE = 0
const val TYPE_ROUTE_ANNOTATION = 1  // @Route(path=xxx)
const val TYPE_THEROUTER_BUILD = 2  // TheRouter.build(xxx)
const val TYPE_ACTION_INTERCEPT = 3  // TheRouter.addActionInterceptor(xxx) || @ActionInterceptor(actionName=xxx)

class CodeWrapper(
    var type: Int = TYPE_NONE,
    var code: String = "",
    var psiElement: PsiElement
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CodeWrapper
        if (type != other.type) return false
        if (getKey(code) != getKey(other.code)) return false
        if (psiElement.getKey() != other.psiElement.getKey()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type + psiElement.getKey().hashCode() + code.hashCode()
        result *= 31
        return result
    }

    private fun getKey(text: String): String {
        return text.replace(" ", "").replace("\n", "")
    }

    private fun getTypeString() = when (type) {
        TYPE_ROUTE_ANNOTATION -> "TYPE_ROUTE_ANNOTATION"
        TYPE_THEROUTER_BUILD -> "TYPE_THEROUTER_BUILD"
        TYPE_ACTION_INTERCEPT -> "TYPE_ACTION_INTERCEPT"
        else -> "TYPE_NONE"
    }

    override fun toString(): String {
        return "type='${getTypeString()}', code='$code', psi='${psiElement.getKey()}'"
    }
}

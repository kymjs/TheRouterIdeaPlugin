package cn.therouter.idea.navigator

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException


open class TargetPsiElement(private val delegate: PsiElement) : PsiElement by delegate {

    fun getKey() = delegate.getKey()

    override fun getText(): String {
        val prefix: String = try {
            val fileName = delegate.containingFile.name
            if (fileName.contains(".")) {
                fileName.substring(0, fileName.indexOf("."))
            } else {
                fileName
            }
        } catch (_: PsiInvalidElementAccessException) {
            ""
        }
        val text = delegate.getKey()
        val suffix = if (text.length > 80) {
            "${text.subSequence(0, 80)}..."
        } else {
            text
        }
        val lineNumber = getElementLineNumbers(delegate)
        val line = if (lineNumber >= 0) {
            "$lineNumber"
        } else {
            ""
        }
        return "$prefix:$line $suffix"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TargetPsiElement

        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        val result = text.hashCode()
        return 31 * result
    }

    fun getElementLineNumbers(psiElement: PsiElement): Int {
        val project: Project = psiElement.project
        val document: Document? = PsiDocumentManager.getInstance(project).getDocument(psiElement.containingFile)
        if (document != null) {
            val textRange: TextRange = psiElement.textRange
            val startLineNumber: Int = document.getLineNumber(textRange.startOffset)
//            val endLineNumber: Int = document.getLineNumber(textRange.endOffset)
            return startLineNumber + 1
        }
        return -1;
    }
}

fun PsiElement.getKey(): String {
    return text.replace(" ", "").replace("\n", "")
}

fun PsiElement.toTargetPsi() = if (this is TargetPsiElement) {
    this
} else {
    TargetPsiElement(this)
}

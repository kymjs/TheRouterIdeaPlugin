package cn.therouter.idea.navigator

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException


open class TargetPsiElement(private val delegate: PsiElement) : Comparable<TargetPsiElement>, PsiElement by delegate {

    fun getKey() = delegate.getKey()

    override fun getText(): String {
        val prefix: String = try {
            val fileName = delegate.getFileName()
            if (fileName.contains(".")) {
                fileName.substring(0, fileName.indexOf("."))
            } else {
                fileName
            }
        } catch (_: PsiInvalidElementAccessException) {
            ""
        }

        val lineNumber = delegate.getLineNumber()
        val line = if (lineNumber >= 0) {
            "$lineNumber"
        } else {
            ""
        }

        val text = delegate.getKey()
        val suffix = if (text.length > 80) {
            "${text.subSequence(0, 80)}..."
        } else {
            text
        }
        return "$prefix:$line $suffix"
    }

    override fun compareTo(other: TargetPsiElement): Int {
        return if (this.getFileName() == other.getFileName()) {
            if (this.getLineNumber() == other.getLineNumber()) {
                this.text.length - other.text.length
            } else {
                this.getLineNumber() - other.getLineNumber()
            }
        } else {
            this.getFileName().compareTo(other.getFileName())
        }
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
}

fun PsiElement.getKey(): String {
    return text.replace(" ", "").replace("\n", "")
}

fun PsiElement.getFileName() = containingFile.name

fun PsiElement.getLineNumber(): Int {
    val document: Document? = PsiDocumentManager.getInstance(project).getDocument(containingFile)
    if (document != null) {
        val textRange: TextRange = textRange
        val startLineNumber: Int = document.getLineNumber(textRange.startOffset)
//            val endLineNumber: Int = document.getLineNumber(textRange.endOffset)
        return startLineNumber + 1
    }
    return -1;
}

fun PsiElement.toTargetPsi() = if (this is TargetPsiElement) {
    this
} else {
    TargetPsiElement(this)
}

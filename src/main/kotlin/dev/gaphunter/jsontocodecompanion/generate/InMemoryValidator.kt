package dev.gaphunter.jsontocodecompanion.generate

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage

/**
 * Same discipline as every other Gap Hunter Labs code-generating
 * plugin: the whole generated file is parsed into a throwaway,
 * never-persisted PSI copy and checked for syntax errors before
 * anything touches disk. Never added to a real
 * [com.intellij.psi.PsiDirectory], zero risk regardless of content.
 */
object InMemoryValidator {

    fun findFirstSyntaxError(project: Project, generatedText: String, fileName: String): String? {
        val language = if (fileName.endsWith(".kt")) KotlinLanguage.INSTANCE else JavaLanguage.INSTANCE
        val psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, language, generatedText, /* eventSystemEnabled = */ false, /* markAsCopy = */ true)
        val firstError = PsiTreeUtil.findChildOfType(psiFile, PsiErrorElement::class.java)
        return firstError?.errorDescription
    }
}

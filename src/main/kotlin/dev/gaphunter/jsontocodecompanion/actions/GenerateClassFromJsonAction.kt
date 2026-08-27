package dev.gaphunter.jsontocodecompanion.actions

import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonObject
import com.intellij.lang.java.JavaLanguage
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import dev.gaphunter.jsontocodecompanion.generate.CodeRenderer
import dev.gaphunter.jsontocodecompanion.generate.InMemoryValidator
import dev.gaphunter.jsontocodecompanion.infer.JsonTypeInferrer
import dev.gaphunter.jsontocodecompanion.model.InferredType
import dev.gaphunter.jsontocodecompanion.review.ReviewPrompt
import org.jetbrains.kotlin.idea.KotlinLanguage

/**
 * Editor context-menu entry point. The selection is always re-parsed
 * as FRESH JSON (via a throwaway [PsiFileFactory]-created file), never
 * read as real JSON PSI at the caret -- that's deliberate: it works
 * identically whether the JSON is sitting in a real `.json` file or
 * pasted as a string literal inside a Java/Kotlin file, matching how
 * "paste JSON as code" tools are actually used in practice.
 */
class GenerateClassFromJsonAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = project != null && editor != null && hasSelection && !DumbService.isDumb(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val invokingFile = e.getData(CommonDataKeys.PSI_FILE)
        val selectedText = editor.selectionModel.selectedText ?: return

        val className = Messages.showInputDialog(
            project, "Root class name:", "Generate Class from JSON", Messages.getQuestionIcon(), "GeneratedClass", null,
        ) ?: return

        val isJava = when (invokingFile?.language) {
            JavaLanguage.INSTANCE -> true
            KotlinLanguage.INSTANCE -> false
            else -> {
                val choice = Messages.showDialog(
                    project, "Generate as which language?", "Generate Class from JSON",
                    arrayOf("Java", "Kotlin"), 0, Messages.getQuestionIcon(),
                )
                if (choice != 0 && choice != 1) return
                choice == 0
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            ApplicationManager.getApplication().runReadAction {
                val jsonFile = PsiFileFactory.getInstance(project)
                    .createFileFromText("__probe.json", JsonLanguage.INSTANCE, selectedText, false, true)
                val rootObject = jsonFile.firstChild as? JsonObject
                if (rootObject == null) {
                    notify(project, "Selected text is not a JSON object -- nothing generated.", NotificationType.WARNING)
                    return@runReadAction
                }

                val rootType = JsonTypeInferrer.infer(rootObject, className) as? InferredType.ObjectType
                if (rootType == null) {
                    notify(project, "Could not infer a class shape from the selection.", NotificationType.WARNING)
                    return@runReadAction
                }

                val targetDirectory = invokingFile?.containingDirectory
                val packageName = targetDirectory?.let { runCatching { JavaDirectoryService.getInstance().getPackage(it)?.qualifiedName }.getOrNull() }

                val fileName = "$className.${if (isJava) "java" else "kt"}"
                val generatedText = if (isJava) CodeRenderer.renderJava(rootType, packageName) else CodeRenderer.renderKotlin(rootType, packageName)

                val error = InMemoryValidator.findFirstSyntaxError(project, generatedText, fileName)
                if (error != null) {
                    notify(project, "Generated class failed its own safety check ($error) -- nothing was written.", NotificationType.ERROR)
                    return@runReadAction
                }

                ApplicationManager.getApplication().invokeLater {
                    writeToDisk(project, targetDirectory, fileName, generatedText, isJava)
                }
            }
        }
    }

    private fun writeToDisk(project: Project, directory: PsiDirectory?, fileName: String, text: String, isJava: Boolean) {
        if (directory == null) {
            notify(project, "Could not resolve a directory to write the generated class into.", NotificationType.ERROR)
            return
        }
        WriteCommandAction.runWriteCommandAction(project, "Generate Class from JSON", null, {
            if (directory.findFile(fileName) != null) {
                notify(project, "$fileName already exists -- not overwriting. Delete it first if you want to regenerate.", NotificationType.WARNING)
                return@runWriteCommandAction
            }
            val language = if (isJava) JavaLanguage.INSTANCE else KotlinLanguage.INSTANCE
            val psiFile: PsiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, language, text)
            directory.add(psiFile)
            // Real success only -- never the "already exists" no-op above.
            ReviewPrompt.recordHit(project)
            notify(project, "$fileName generated.", NotificationType.INFORMATION)
        })
    }

    private fun notify(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("JSON to Code Companion")
            .createNotification(message, type)
            .notify(project)
    }
}

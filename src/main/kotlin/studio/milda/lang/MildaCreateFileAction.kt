package studio.milda.lang

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import studio.milda.MildaIcons

/**
 * *New | Milda File* in the Project view's context menu.
 *
 * A language the IDE recognises but cannot CREATE is only half-installed: the file has to be
 * made somewhere else and dragged in, and the first thing an author wants to do with a new
 * design system is start one. The template it writes (fileTemplates/internal) is a small but
 * complete system - a context group, tokens, an alias that covers its group - so the new file
 * parses cleanly and shows the shape of the thing rather than being an empty buffer.
 *
 * The `.milda` extension is supplied by the template's own name, so a name typed with or
 * without it lands on one file rather than `tokens.milda.milda`.
 */
class MildaCreateFileAction : CreateFileFromTemplateAction("Milda File", "A design system in Milda", MildaIcons.FILE) {
    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String =
        "Create Milda File"

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle("New Milda File")
            .addKind("Milda file", MildaIcons.FILE, TEMPLATE_NAME)
    }

    companion object {
        /** Must match the file name under resources/fileTemplates/internal (minus `.milda.ft`). */
        const val TEMPLATE_NAME: String = "Milda File"
    }
}

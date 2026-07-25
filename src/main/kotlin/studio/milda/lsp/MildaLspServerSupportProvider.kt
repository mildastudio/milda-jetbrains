package studio.milda.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.LspServerSupportProvider.LspServerStarter

/** The `.milda` file extension the language server handles. */
internal const val MILDA_EXTENSION = "milda"

/**
 * Entry point the IDE calls as files open. For a `.milda` file it ensures the shared,
 * project-wide Milda language server is running (started once, reused for every
 * `.milda` file in the project).
 */
class MildaLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerStarter) {
        if (file.extension == MILDA_EXTENSION) {
            serverStarter.ensureServerStarted(MildaLspServerDescriptor(project))
        }
    }
}

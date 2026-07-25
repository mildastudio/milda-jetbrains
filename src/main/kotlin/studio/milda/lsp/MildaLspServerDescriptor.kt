package studio.milda.lsp

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor

/**
 * Describes how to launch and scope the Milda language server. Project-wide: one
 * server instance serves every `.milda` file in the project. The platform reads the
 * server's advertised capabilities (diagnostics, symbols, folding, completion, hover,
 * definition, semantic tokens) automatically — nothing to declare here.
 */
class MildaLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "Milda") {

    override fun isSupportedFile(file: VirtualFile): Boolean = file.extension == MILDA_EXTENSION

    override fun createCommandLine(): GeneralCommandLine {
        val settings = MildaLspSettings.getInstance()
        val node = settings.nodeExecutable.ifBlank { "node" }
        val serverScript = MildaServerLocator.locate(project, settings)
            ?: throw ExecutionException(
                "Milda language server not found. Install it in the project " +
                    "(npm i -D @mildastudio/milda-lsp) or set an explicit path in " +
                    "Settings | Tools | Milda.",
            )
        return GeneralCommandLine(node, serverScript)
            .withWorkDirectory(project.basePath)
            .withCharset(Charsets.UTF_8)
    }
}

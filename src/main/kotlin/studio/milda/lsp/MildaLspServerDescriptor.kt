package studio.milda.lsp

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCustomization
import com.intellij.platform.lsp.api.customization.LspSemanticTokensCustomizer
import com.intellij.platform.lsp.api.customization.LspSemanticTokensDisabled

/**
 * Describes how to launch and scope the Milda language server. Project-wide: one
 * server instance serves every `.milda` file in the project. The platform reads the
 * server's advertised capabilities (diagnostics, symbols, folding, completion, hover,
 * definition, signature help) automatically - nothing to declare here.
 */
class MildaLspServerDescriptor(project: Project) :
    ProjectWideLspServerDescriptor(project, "Milda") {

    override fun isSupportedFile(file: VirtualFile): Boolean = file.extension == MILDA_EXTENSION

    /**
     * Colouring stays with the plugin's own lexer, which is a verified twin of the server's
     * classifier (MildaLexerConformanceTest) - so nothing is lost - and whose categories have
     * colours we control (MildaSyntaxHighlighter + resources/colorSchemes).
     *
     * The platform's LSP semantic-token layer would paint OVER those, through a fixed mapping
     * we cannot influence: `type` → `DefaultLanguageHighlighterColors.IDENTIFIER`, i.e. the
     * plain foreground (read off `LspSemanticTokensSupport.getTextAttributesKey` in
     * 2026.1). That is why a token's declared type looked uncoloured however the plugin
     * classified it. The server still answers `textDocument/semanticTokens/full` for editors
     * whose mapping is theirs to theme (VS Code, Neovim, Zed).
     */
    override val lspCustomization: LspCustomization = object : LspCustomization() {
        override val semanticTokensCustomizer: LspSemanticTokensCustomizer = LspSemanticTokensDisabled
    }

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

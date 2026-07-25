package studio.milda.lsp

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

/** Settings | Tools | Milda — configure the Node runtime and server script path. */
class MildaLspConfigurable : BoundConfigurable("Milda") {
    private val settings = MildaLspSettings.getInstance()

    override fun createPanel(): DialogPanel = panel {
        row("Node executable:") {
            textField()
                .bindText(settings::nodeExecutable)
                .comment("Path to the Node.js runtime, or just \"node\" if it's on PATH.")
        }
        row("Server script path:") {
            textField()
                .bindText(settings::serverScriptPath)
                .comment(
                    "Absolute path to the milda-lsp entry (dist/index.cjs). Leave blank to " +
                        "auto-detect from the project's node_modules.",
                )
        }
    }
}

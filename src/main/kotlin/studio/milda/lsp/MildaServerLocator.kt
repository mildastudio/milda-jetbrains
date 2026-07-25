package studio.milda.lsp

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Finds the language server's entry script. Resolution order:
 *   1. an explicit path from settings (if it exists),
 *   2. the project's `node_modules` (a local `@mildastudio/milda-lsp` install),
 *   3. the server bundled inside the plugin (extracted to the IDE system dir).
 * A user only needs Node.js on their machine; the server itself ships with the plugin.
 * Returns null only if even the bundled copy is missing (shouldn't happen in a release).
 */
object MildaServerLocator {
    fun locate(project: Project, settings: MildaLspSettings): String? {
        settings.serverScriptPath
            .takeIf { it.isNotBlank() }
            ?.let { path -> if (File(path).isFile) return path }

        val base = project.basePath
        if (base != null) {
            listOf(
                "$base/node_modules/@mildastudio/milda-lsp/dist/index.cjs",
                "$base/node_modules/.bin/milda-lsp",
            ).firstOrNull { File(it).isFile }?.let { return it }
        }

        return bundledServer()
    }

    // Extract the plugin-bundled server to a stable file the OS can execute (Node can't
    // run from inside the plugin jar). Rewritten on each resolve so a plugin update always
    // ships its matching server.
    private fun bundledServer(): String? {
        val stream = MildaServerLocator::class.java.getResourceAsStream("/server/milda-lsp.cjs") ?: return null
        val dir = Path.of(PathManager.getSystemPath(), "milda-lsp")
        Files.createDirectories(dir)
        val out = dir.resolve("milda-lsp.cjs")
        stream.use { input -> Files.newOutputStream(out).use { output -> input.copyTo(output) } }
        return out.toString()
    }
}

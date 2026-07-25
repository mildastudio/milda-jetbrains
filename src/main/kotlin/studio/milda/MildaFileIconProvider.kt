package studio.milda

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

// Gives `.milda` files their own icon in the project view / editor tabs — purely
// cosmetic. Unlike a FileType + ParserDefinition, this touches nothing about the
// language, parsing, or highlighting, so LSP semantic-token coloring is unaffected.
class MildaFileIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? =
        if (file.extension == "milda") MildaIcons.FILE else null
}

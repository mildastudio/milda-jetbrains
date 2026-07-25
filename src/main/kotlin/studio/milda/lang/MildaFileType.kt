package studio.milda.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import studio.milda.MildaIcons
import javax.swing.Icon

object MildaFileType : LanguageFileType(MildaLanguage) {
    override fun getName(): String = "Milda"
    override fun getDescription(): String = "Milda design-system DSL"
    override fun getDefaultExtension(): String = "milda"
    override fun getIcon(): Icon = MildaIcons.FILE
}

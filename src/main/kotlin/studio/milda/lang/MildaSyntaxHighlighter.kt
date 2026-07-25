package studio.milda.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class MildaSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = MildaHighlightLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        ATTRS[tokenType]?.let { arrayOf(it) } ?: TextAttributesKey.EMPTY_ARRAY

    companion object {
        private fun key(name: String, fallback: TextAttributesKey) =
            TextAttributesKey.createTextAttributesKey("MILDA_$name", fallback)

        private val ATTRS: Map<IElementType, TextAttributesKey> = mapOf(
            MildaTypes.KEYWORD to key("KEYWORD", DefaultLanguageHighlighterColors.KEYWORD),
            MildaTypes.TYPE to key("TYPE", DefaultLanguageHighlighterColors.CLASS_NAME),
            MildaTypes.STRING to key("STRING", DefaultLanguageHighlighterColors.STRING),
            MildaTypes.NUMBER to key("NUMBER", DefaultLanguageHighlighterColors.NUMBER),
            MildaTypes.COMMENT to key("COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT),
            MildaTypes.PROPERTY to key("PROPERTY", DefaultLanguageHighlighterColors.INSTANCE_FIELD),
            MildaTypes.VARIABLE to key("VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE),
            MildaTypes.OPERATOR to key("OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN),
        )
    }
}

class MildaSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        MildaSyntaxHighlighter()
}

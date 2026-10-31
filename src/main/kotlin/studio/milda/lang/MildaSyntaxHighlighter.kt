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

        // Named so they can be found: these are the keys the colour scheme files
        // (resources/colorSchemes) give explicit colours to, and the ones MildaColorSettingsPage
        // puts in *Settings | Editor | Color Scheme | Milda*.
        //
        // TYPE and DECLARATION carry explicit colours because their FALLBACKS do not work:
        // `CLASS_NAME` and `IDENTIFIER` are the plain foreground in most dark schemes, so a
        // token's type and a token's name came out the same colour as everything else - which
        // is what a reader complained about. The rest keep the scheme's own keys, which do have
        // colours everywhere (keyword, number, string, comment, field).
        val KEYWORD = key("KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val TYPE = key("TYPE", DefaultLanguageHighlighterColors.CLASS_NAME)
        val DECLARATION = key("DECLARATION", DefaultLanguageHighlighterColors.CLASS_NAME)
        val STRING = key("STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = key("NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val COMMENT = key("COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val PROPERTY = key("PROPERTY", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
        val VARIABLE = key("VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
        val OPERATOR = key("OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)

        private val ATTRS: Map<IElementType, TextAttributesKey> = mapOf(
            MildaTypes.KEYWORD to KEYWORD,
            MildaTypes.TYPE to TYPE,
            MildaTypes.DECLARATION to DECLARATION,
            MildaTypes.STRING to STRING,
            MildaTypes.NUMBER to NUMBER,
            MildaTypes.COMMENT to COMMENT,
            MildaTypes.PROPERTY to PROPERTY,
            MildaTypes.VARIABLE to VARIABLE,
            MildaTypes.OPERATOR to OPERATOR,
        )
    }
}

class MildaSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        MildaSyntaxHighlighter()
}

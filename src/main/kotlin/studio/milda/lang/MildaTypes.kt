package studio.milda.lang

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

class MildaTokenType(debugName: String) : IElementType(debugName, MildaLanguage)

object MildaTypes {
    @JvmField val KEYWORD = MildaTokenType("KEYWORD")
    @JvmField val TYPE = MildaTokenType("TYPE")
    @JvmField val STRING = MildaTokenType("STRING")
    @JvmField val NUMBER = MildaTokenType("NUMBER")
    @JvmField val COMMENT = MildaTokenType("COMMENT")
    @JvmField val PROPERTY = MildaTokenType("PROPERTY")
    @JvmField val VARIABLE = MildaTokenType("VARIABLE")
    @JvmField val OPERATOR = MildaTokenType("OPERATOR")
    @JvmField val IDENTIFIER = MildaTokenType("IDENTIFIER") // filler (uncoloured)
    @JvmField val FILE = IFileElementType(MildaLanguage)

    // Map a scanner category to the IElementType the lexer/highlighter uses.
    fun forCategory(category: String): IElementType = when (category) {
        MildaHighlightScanner.KEYWORD -> KEYWORD
        MildaHighlightScanner.TYPE -> TYPE
        MildaHighlightScanner.STRING -> STRING
        MildaHighlightScanner.NUMBER -> NUMBER
        MildaHighlightScanner.COMMENT -> COMMENT
        MildaHighlightScanner.PROPERTY -> PROPERTY
        MildaHighlightScanner.VARIABLE -> VARIABLE
        MildaHighlightScanner.OPERATOR -> OPERATOR
        MildaHighlightScanner.WHITESPACE -> TokenType.WHITE_SPACE
        else -> IDENTIFIER
    }
}

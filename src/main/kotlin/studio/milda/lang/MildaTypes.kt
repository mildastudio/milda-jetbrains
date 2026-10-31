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
    @JvmField val DECLARATION = MildaTokenType("DECLARATION") // a name this file declares
    @JvmField val IDENTIFIER = MildaTokenType("IDENTIFIER") // filler (uncoloured)

    // The paired punctuation gets its own token types - uncoloured like any other filler,
    // but distinguishable, which is what MildaBraceMatcher needs to match a pair (and what
    // makes the platform auto-insert the closing brace as you type the opening one).
    @JvmField val LBRACE = MildaTokenType("LBRACE")
    @JvmField val RBRACE = MildaTokenType("RBRACE")
    @JvmField val LPAREN = MildaTokenType("LPAREN")
    @JvmField val RPAREN = MildaTokenType("RPAREN")
    @JvmField val LBRACKET = MildaTokenType("LBRACKET")
    @JvmField val RBRACKET = MildaTokenType("RBRACKET")

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
        MildaHighlightScanner.DECLARATION -> DECLARATION
        MildaHighlightScanner.WHITESPACE -> TokenType.WHITE_SPACE
        MildaHighlightScanner.LBRACE -> LBRACE
        MildaHighlightScanner.RBRACE -> RBRACE
        MildaHighlightScanner.LPAREN -> LPAREN
        MildaHighlightScanner.RPAREN -> RPAREN
        MildaHighlightScanner.LBRACKET -> LBRACKET
        MildaHighlightScanner.RBRACKET -> RBRACKET
        else -> IDENTIFIER
    }
}

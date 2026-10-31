package studio.milda.lang

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

// Declaring the language's brace pairs buys three things at once, all of which a reader
// expects from any language in the IDE: matched braces highlight when the caret is on one,
// the closing brace is auto-inserted as you type the opening one (with the caret left
// between them), and an unbalanced brace is reported.
//
// `{ … }` is marked STRUCTURAL: every Milda block (foundations, component, part, layout, …)
// is brace-delimited, so a `{` that never closes should end the enclosing block rather than
// swallow the rest of the file.
class MildaBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = PAIRS

    // Milda has no token after which a pair would be unwanted (no generics ambiguity, no
    // string interpolation), so the closing brace is always welcome.
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    // Used when collapsing a block: the construct starts at its own brace, since the PSI is
    // flat (the LSP, not a grammar, is what understands structure here).
    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS = arrayOf(
            BracePair(MildaTypes.LBRACE, MildaTypes.RBRACE, true),
            BracePair(MildaTypes.LPAREN, MildaTypes.RPAREN, false),
            BracePair(MildaTypes.LBRACKET, MildaTypes.RBRACKET, false),
        )
    }
}

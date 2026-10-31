package studio.milda.lang

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.psi.tree.TokenSet

// Milda's only quote is the double quote around a string literal (descriptions, escape
// hatches, metadata), so typing one closes it and typing over the closing one steps out -
// the same behaviour every other language in the IDE has.
class MildaQuoteHandler : SimpleTokenSetQuoteHandler(TokenSet.create(MildaTypes.STRING))

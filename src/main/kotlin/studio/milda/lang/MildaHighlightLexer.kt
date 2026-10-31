package studio.milda.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

// Thin IntelliJ-Lexer adapter over the pure MildaHighlightScanner (all the real logic - and
// the parity guarantee - lives there).
//
// The state is the whole point of this class. Milda's colouring is CONTEXT-DEPENDENT - a word
// after a `:` is a type, a word after `token` is a declared name, a word inside `hex(…)` is a
// value - and the platform re-lexes only the region around an edit, restarting from the state
// it cached at some earlier token. A lexer that always reported state 0 told the platform "you
// can restart anywhere and get the same answer", which was not true: re-lexing `white` without
// having read the `token` in front of it lost the declaration colour, and no further edit
// brought it back (every re-lex restarted from the same place). So `getState` reports the state
// each token STARTS in, and `start` resumes from the state it is handed.
class MildaHighlightLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var spans: List<MildaHighlightScanner.Span> = emptyList()
    private var index = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        spans = MildaHighlightScanner.scan(buffer.subSequence(startOffset, endOffset), initialState)
            .map { MildaHighlightScanner.Span(it.start + startOffset, it.end + startOffset, it.category, it.stateBefore) }
        index = 0
    }

    /** The state the CURRENT token starts in - what `start` must be handed to resume here. */
    override fun getState(): Int = if (index >= spans.size) 0 else spans[index].stateBefore

    override fun getTokenType(): IElementType? =
        if (index >= spans.size) null else MildaTypes.forCategory(spans[index].category)
    override fun getTokenStart(): Int = if (index >= spans.size) bufferEnd else spans[index].start
    override fun getTokenEnd(): Int = if (index >= spans.size) bufferEnd else spans[index].end
    override fun advance() { index++ }
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = bufferEnd
}

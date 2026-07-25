package studio.milda.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

// Thin IntelliJ-Lexer adapter over the pure MildaHighlightScanner (all the real logic —
// and the parity guarantee — lives there). Restartable: re-scans the [start, end) region.
class MildaHighlightLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var spans: List<MildaHighlightScanner.Span> = emptyList()
    private var index = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        spans = MildaHighlightScanner.scan(buffer.subSequence(startOffset, endOffset))
            .map { MildaHighlightScanner.Span(it.start + startOffset, it.end + startOffset, it.category) }
        index = 0
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? =
        if (index >= spans.size) null else MildaTypes.forCategory(spans[index].category)
    override fun getTokenStart(): Int = if (index >= spans.size) bufferEnd else spans[index].start
    override fun getTokenEnd(): Int = if (index >= spans.size) bufferEnd else spans[index].end
    override fun advance() { index++ }
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = bufferEnd
}

package studio.milda.lang

// Pure, platform-free highlight tokenizer. This is the Kotlin twin of the TS classifier
// (packages/milda-lsp/src/semantic-tokens.ts `scan`/`classify`) and MUST stay in lockstep
// with it — parity is enforced by MildaLexerConformanceTest, which runs this scanner over
// the shared corpus and compares against TS-generated goldens.
//
// It emits contiguous spans covering the whole buffer (whitespace/other filler included)
// so MildaHighlightLexer can adapt it to IntelliJ's Lexer contract; the 8 coloured
// categories match semantic-tokens.ts's SEMANTIC_TOKEN_TYPES.
object MildaHighlightScanner {
    data class Span(val start: Int, val end: Int, val category: String)

    const val KEYWORD = "keyword"
    const val TYPE = "type"
    const val STRING = "string"
    const val NUMBER = "number"
    const val COMMENT = "comment"
    const val PROPERTY = "property"
    const val VARIABLE = "variable"
    const val OPERATOR = "operator"
    const val WHITESPACE = "whitespace" // filler (Lexer needs it; goldens/highlighter ignore)
    const val OTHER = "other" // filler (unclassified words + punctuation)

    val HIGHLIGHT_CATEGORIES = setOf(KEYWORD, TYPE, STRING, NUMBER, COMMENT, PROPERTY, VARIABLE, OPERATOR)

    private val NUMBER_RE = Regex("^-?\\d+(\\.\\d+)?[a-z%]*$")

    private fun isWordChar(c: Char): Boolean =
        (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') ||
            c == '_' || c == '.' || c == '/' || c == '%' || c == '$' || c == '-'

    fun scan(text: CharSequence): List<Span> {
        val out = ArrayList<Span>()
        val n = text.length
        var i = 0
        while (i < n) {
            val c = text[i]
            when {
                c == ' ' || c == '\t' || c == '\r' || c == '\n' -> {
                    var j = i + 1
                    while (j < n && (text[j] == ' ' || text[j] == '\t' || text[j] == '\r' || text[j] == '\n')) j++
                    out.add(Span(i, j, WHITESPACE)); i = j
                }
                c == '#' -> {
                    var j = i + 1
                    while (j < n && text[j] != '\n') j++
                    out.add(Span(i, j, COMMENT)); i = j
                }
                c == '"' -> {
                    var j = i + 1
                    while (j < n && text[j] != '"' && text[j] != '\n') {
                        if (text[j] == '\\') j++
                        j++
                    }
                    if (j < n && text[j] == '"') j++
                    out.add(Span(i, j, STRING)); i = j
                }
                c == '-' && i + 1 < n && text[i + 1] == '>' -> { out.add(Span(i, i + 2, OPERATOR)); i += 2 }
                c == '→' -> { out.add(Span(i, i + 1, OPERATOR)); i += 1 } // unicode arrow →
                (c == '<' || c == '>' || c == '!') && i + 1 < n && text[i + 1] == '=' -> { out.add(Span(i, i + 2, OPERATOR)); i += 2 }
                c == '=' || c == ':' -> { out.add(Span(i, i + 1, OPERATOR)); i += 1 }
                isWordChar(c) -> {
                    var j = i
                    while (j < n && isWordChar(text[j]) && !(text[j] == '-' && j + 1 < n && text[j + 1] == '>')) j++
                    out.add(Span(i, j, classify(text.subSequence(i, j).toString()))); i = j
                }
                else -> { out.add(Span(i, i + 1, OTHER)); i += 1 }
            }
        }
        return out
    }

    private fun classify(word: String): String = when {
        NUMBER_RE.matches(word) -> NUMBER
        MildaVocab.KEYWORDS.contains(word) -> KEYWORD
        MildaVocab.TYPE_WORDS.contains(word) || MildaVocab.COLOR_FNS.contains(word) -> TYPE
        word.startsWith("$") -> VARIABLE
        word.contains(".") -> PROPERTY
        else -> OTHER
    }
}

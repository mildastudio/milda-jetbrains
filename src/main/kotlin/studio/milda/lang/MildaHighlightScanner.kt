package studio.milda.lang

// Pure, platform-free highlight tokenizer. This is the Kotlin twin of the TS classifier
// (packages/milda-lsp/src/semantic-tokens.ts `scan`/`classify`) and MUST stay in lockstep
// with it - parity is enforced by MildaLexerConformanceTest, which runs this scanner over
// the shared corpus and compares against TS-generated goldens.
//
// It emits contiguous spans covering the whole buffer (whitespace/other filler included)
// so MildaHighlightLexer can adapt it to IntelliJ's Lexer contract; the 8 coloured
// categories match semantic-tokens.ts's SEMANTIC_TOKEN_TYPES.
object MildaHighlightScanner {
    /** `stateBefore` is the scanner state this span STARTS in - what an IntelliJ Lexer must
     *  report so the platform can restart mid-file and get the same answer (see
     *  MildaHighlightLexer). Kotlin-only: the TS classifier always sees a whole document. */
    data class Span(val start: Int, val end: Int, val category: String, val stateBefore: Int = 0)

    const val KEYWORD = "keyword"
    const val TYPE = "type"
    const val STRING = "string"
    const val NUMBER = "number"
    const val COMMENT = "comment"
    const val PROPERTY = "property"
    const val VARIABLE = "variable"
    const val OPERATOR = "operator"

    /** A name this file DECLARES. The string is the LSP semantic-token type the TS classifier
     *  emits (`class` is the standard type nearest to "an entity declared here"), so the
     *  goldens compare directly. */
    const val DECLARATION = "class"
    const val WHITESPACE = "whitespace" // filler (Lexer needs it; goldens/highlighter ignore)
    const val OTHER = "other" // filler (unclassified words + punctuation)

    // Paired punctuation, split out of OTHER. Still filler as far as colouring goes - the
    // TS classifier emits nothing at all for these positions, so there is no golden to
    // drift from - but the IDE needs each side of a pair as its own token to match braces
    // and to auto-insert the closing one (MildaBraceMatcher).
    const val LBRACE = "lbrace"
    const val RBRACE = "rbrace"
    const val LPAREN = "lparen"
    const val RPAREN = "rparen"
    const val LBRACKET = "lbracket"
    const val RBRACKET = "rbracket"

    val HIGHLIGHT_CATEGORIES =
        setOf(KEYWORD, TYPE, STRING, NUMBER, COMMENT, PROPERTY, VARIABLE, OPERATOR, DECLARATION)

    private val NUMBER_RE = Regex("^-?\\d+(\\.\\d+)?[a-z%]*$")

    private fun isWordChar(c: Char): Boolean =
        (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') ||
            c == '_' || c == '.' || c == '/' || c == '%' || c == '$' || c == '-'

    private val LITERAL_FNS: Set<String> = MildaVocab.COLOR_FNS + MildaVocab.LENGTH_FNS

    // ─── the scanner's state, as one int ──────────────────────────────────────
    // The three positional rules make this scanner CONTEXT-DEPENDENT: what a word is coloured
    // as depends on what came before it on the line. An IntelliJ Lexer that hides that fact
    // gets silently wrong answers, because the platform re-lexes only the region around an
    // edit and restarts from a cached state - so `token white` re-lexed from `white` alone lost
    // the fact that it is a declaration, and typing a space anywhere on the line was enough to
    // lose the colour for good. Packing the state into the int the Lexer contract already
    // carries is what makes a restart mid-line come out the same as a scan from the top.
    private const val AFTER_COLON = 1
    private const val AFTER_DECL = 2
    private const val PENDING_FN = 4
    private const val ARG_DEPTH_SHIFT = 3
    private const val ARG_DEPTH_MAX = 7 // a literal's parens do not nest deeply; clamped

    private fun pack(argDepth: Int, pendingFn: Boolean, afterColon: Boolean, afterDecl: Boolean): Int =
        (if (afterColon) AFTER_COLON else 0) or
            (if (afterDecl) AFTER_DECL else 0) or
            (if (pendingFn) PENDING_FN else 0) or
            (minOf(argDepth, ARG_DEPTH_MAX) shl ARG_DEPTH_SHIFT)

    fun scan(text: CharSequence): List<Span> = scan(text, 0)

    /** Scan `text`, resuming from `initialState` (0 = the start of a document). */
    fun scan(text: CharSequence, initialState: Int): List<Span> {
        val out = ArrayList<Span>()
        val n = text.length
        var i = 0
        // The pieces of position the classifier needs (all reset at end-of-line, so a stray `(`
        // or `:` can never repaint the rest of the file). Kept in lockstep with the TS scan's
        // own state - see semantic-tokens.ts for why each exists.
        var argDepth = (initialState shr ARG_DEPTH_SHIFT) and ARG_DEPTH_MAX
        var pendingFn = (initialState and PENDING_FN) != 0
        var afterColon = (initialState and AFTER_COLON) != 0
        var afterDecl = (initialState and AFTER_DECL) != 0
        while (i < n) {
            val entryState = pack(argDepth, pendingFn, afterColon, afterDecl)
            val c = text[i]
            // Whitespace first, and without touching the state: a literal may be written
            // `hex (fff)` and a type `x : color`, so a gap must not end either.
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                var j = i + 1
                while (j < n && (text[j] == ' ' || text[j] == '\t' || text[j] == '\r' || text[j] == '\n')) j++
                // A newline ends the statement: forget where in it we were.
                if (text.subSequence(i, j).contains('\n')) {
                    argDepth = 0; pendingFn = false; afterColon = false; afterDecl = false
                }
                out.add(Span(i, j, WHITESPACE, entryState)); i = j
                continue
            }
            // Every other token consumes the state, so it is read and cleared in ONE place -
            // a branch that forgot to clear it would silently paint the rest of the line.
            val inArgs = argDepth > 0
            val wasPendingFn = pendingFn
            val wasAfterColon = afterColon
            val wasAfterDecl = afterDecl
            pendingFn = false
            afterColon = false
            afterDecl = false
            when {
                c == '(' -> {
                    if (wasPendingFn) argDepth = 1 else if (argDepth > 0) argDepth++
                    out.add(Span(i, i + 1, LPAREN, entryState)); i += 1
                }
                c == ')' -> {
                    if (argDepth > 0) argDepth--
                    out.add(Span(i, i + 1, RPAREN, entryState)); i += 1
                }
                c == '#' -> {
                    var j = i + 1
                    while (j < n && text[j] != '\n') j++
                    out.add(Span(i, j, COMMENT, entryState)); i = j
                }
                c == '"' -> {
                    var j = i + 1
                    while (j < n && text[j] != '"' && text[j] != '\n') {
                        if (text[j] == '\\') j++
                        j++
                    }
                    if (j < n && text[j] == '"') j++
                    out.add(Span(i, j, STRING, entryState)); i = j
                }
                c == '-' && i + 1 < n && text[i + 1] == '>' -> { out.add(Span(i, i + 2, OPERATOR, entryState)); i += 2 }
                c == '→' -> { out.add(Span(i, i + 1, OPERATOR, entryState)); i += 1 } // unicode arrow →
                (c == '<' || c == '>' || c == '!') && i + 1 < n && text[i + 1] == '=' -> { out.add(Span(i, i + 2, OPERATOR, entryState)); i += 2 }
                c == '=' || c == ':' -> {
                    afterColon = c == ':'
                    out.add(Span(i, i + 1, OPERATOR, entryState)); i += 1
                }
                isWordChar(c) -> {
                    var j = i
                    while (j < n && isWordChar(text[j]) && !(text[j] == '-' && j + 1 < n && text[j + 1] == '>')) j++
                    val word = text.subSequence(i, j).toString()
                    out.add(Span(i, j, classify(word, inArgs, wasAfterColon, wasAfterDecl), entryState))
                    pendingFn = LITERAL_FNS.contains(word)
                    afterDecl = MildaVocab.DECLARATION_KEYWORDS.contains(word)
                    i = j
                }
                else -> { out.add(Span(i, i + 1, punctuation(c), entryState)); i += 1 }
            }
        }
        return out
    }

    private fun punctuation(c: Char): String = when (c) {
        '{' -> LBRACE
        '}' -> RBRACE
        '(' -> LPAREN
        ')' -> RPAREN
        '[' -> LBRACKET
        ']' -> RBRACKET
        else -> OTHER
    }

    /** One word to one category. `inArgs` = inside a value literal's parentheses, `afterColon`
     *  = the word follows a `:`, `afterDecl` = it follows a declaring keyword; the three facts
     *  a word list alone cannot supply. */
    private fun classify(word: String, inArgs: Boolean, afterColon: Boolean, afterDecl: Boolean): String = when {
        // Inside `hex(…)` / `oklch(…)` / `scaled(…)` every word is the literal's value,
        // digits or not: `fff` is as numeric as `000`.
        inArgs -> NUMBER
        NUMBER_RE.matches(word) -> NUMBER
        MildaVocab.KEYWORDS.contains(word) -> KEYWORD
        MildaVocab.TYPE_WORDS.contains(word) || MildaVocab.COLOR_FNS.contains(word) ||
            MildaVocab.LENGTH_FNS.contains(word) -> TYPE
        // A declared type: `token brand : color`, `size : Size`, `field : string`.
        afterColon -> TYPE
        // A name this file declares. Before the dotted rule: a part's name IS a dotted path.
        afterDecl -> DECLARATION
        word.startsWith("$") -> VARIABLE
        word.contains(".") -> PROPERTY
        else -> OTHER
    }
}

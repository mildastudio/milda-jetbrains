package studio.milda.lang

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The paired punctuation is what brace matching and auto-insertion are built on: the
// scanner has to hand each side of a pair out as its own token, the pairs the matcher
// declares have to be exactly those tokens, and none of it may leak into colouring (the
// TS classifier emits nothing at these positions - see MildaLexerConformanceTest).
class MildaBracesTest {
    @Test
    fun scannerSeparatesEachSideOfAPair() {
        val categories = MildaHighlightScanner.scan("{}()[]").map { it.category }
        assertEquals(
            listOf(
                MildaHighlightScanner.LBRACE, MildaHighlightScanner.RBRACE,
                MildaHighlightScanner.LPAREN, MildaHighlightScanner.RPAREN,
                MildaHighlightScanner.LBRACKET, MildaHighlightScanner.RBRACKET,
            ),
            categories,
        )
    }

    @Test
    fun punctuationIsNotColoured() {
        val text = "foundations { token a : color = hex(fff) }"
        for (span in MildaHighlightScanner.scan(text)) {
            val chunk = text.substring(span.start, span.end)
            if (chunk.length == 1 && chunk[0] in "{}()[]") {
                assertFalse(
                    "$chunk must stay uncoloured, got ${span.category}",
                    span.category in MildaHighlightScanner.HIGHLIGHT_CATEGORIES,
                )
            }
        }
        // A brace inside a comment or a string is part of that token, never a brace.
        assertTrue(
            MildaHighlightScanner.scan("# a { brace").none { it.category == MildaHighlightScanner.LBRACE },
        )
        assertTrue(
            MildaHighlightScanner.scan("\"a { brace\"").none { it.category == MildaHighlightScanner.LBRACE },
        )
    }

    @Test
    fun matcherPairsTheScannersTokens() {
        val pairs = MildaBraceMatcher().pairs.associate { it.leftBraceType to it.rightBraceType }
        assertEquals(MildaTypes.RBRACE, pairs[MildaTypes.LBRACE])
        assertEquals(MildaTypes.RPAREN, pairs[MildaTypes.LPAREN])
        assertEquals(MildaTypes.RBRACKET, pairs[MildaTypes.LBRACKET])
        // Blocks are brace-delimited, so `{ … }` is the structural pair.
        assertTrue(MildaBraceMatcher().pairs.single { it.leftBraceType == MildaTypes.LBRACE }.isStructural)
    }

    @Test
    fun everyScannerCategoryMapsToATokenType() {
        val categories = MildaHighlightScanner.scan("foundations { token a : color = hex(fff) } # c")
            .map { it.category }
            .toSet()
        for (category in categories) {
            assertTrue("category $category maps", MildaTypes.forCategory(category).toString().isNotEmpty())
        }
    }
}

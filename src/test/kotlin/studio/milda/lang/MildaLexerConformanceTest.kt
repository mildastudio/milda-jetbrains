package studio.milda.lang

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Parity gate: the Kotlin highlight scanner must classify the shared corpus EXACTLY like
// the TS classifier (packages/milda-lsp/src/semantic-tokens.ts), verified against the
// TS-generated goldens (highlight-corpus/*.tokens.json). If the TS side changes, its
// golden-freshness test forces regenerating the goldens, and this test then catches any
// Kotlin drift. Uses the pure scanner (no IDE runtime needed).
class MildaLexerConformanceTest {
    private data class Tok(val line: Int, val char: Int, val length: Int, val type: String)

    @Test
    fun kotlinScannerMatchesTsGoldens() {
        val dir = File("../milda-lsp/highlight-corpus")
        assertTrue("corpus dir exists: ${dir.absolutePath}", dir.isDirectory)
        val files = dir.listFiles { f -> f.name.endsWith(".milda") }!!.sortedBy { it.name }
        assertTrue("corpus is non-empty", files.isNotEmpty())

        val gson = Gson()
        for (f in files) {
            val text = f.readText()
            val actual = classifiedTokens(text)
            val goldenFile = File(dir, f.name.removeSuffix(".milda") + ".tokens.json")
            val golden = gson.fromJson(goldenFile.readText(), Array<Tok>::class.java).toList()
            assertEquals("highlight tokens for ${f.name}", golden, actual)
        }
    }

    private fun classifiedTokens(text: String): List<Tok> {
        val lineStarts = ArrayList<Int>()
        lineStarts.add(0)
        for (i in text.indices) if (text[i] == '\n') lineStarts.add(i + 1)
        fun lineOf(offset: Int): Int {
            var lo = 0
            var hi = lineStarts.size - 1
            while (lo < hi) {
                val mid = (lo + hi + 1) / 2
                if (lineStarts[mid] <= offset) lo = mid else hi = mid - 1
            }
            return lo
        }
        return MildaHighlightScanner.scan(text)
            .filter { it.category in MildaHighlightScanner.HIGHLIGHT_CATEGORIES }
            .map { s ->
                val ln = lineOf(s.start)
                Tok(ln, s.start - lineStarts[ln], s.end - s.start, s.category)
            }
    }
}

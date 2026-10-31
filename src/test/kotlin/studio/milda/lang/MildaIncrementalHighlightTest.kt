package studio.milda.lang

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.testFramework.fixtures.BasePlatformTestCase

// Colouring has to survive an edit.
//
// Reported from a real session: adding a space in `token white : color = hex(fff)` - between the
// name and the colon - made `white` lose its declared-name colour, and deleting the space again
// did not bring it back. The cause is not in the classifier: it is that the platform re-lexes
// only the region around an edit, restarting from the state it cached at an earlier token, and
// this language's colouring depends on what came before on the line (a word after `token` is a
// name, a word after `:` is a type, a word inside `hex(…)` is a value). A lexer that reports
// state 0 everywhere claims that context does not exist.
//
// These cases drive the REAL editor highlighter, which is the thing that was broken - the pure
// scanner was right all along, and a scanner-level test would have passed.
class MildaIncrementalHighlightTest : BasePlatformTestCase() {
    private fun tokenTypeAt(offset: Int) =
        ((myFixture.editor as EditorEx).highlighter.createIterator(offset)).tokenType

    private fun assertDeclaration(word: String) {
        val offset = myFixture.editor.document.text.indexOf(word)
        assertTrue("`$word` must be in the document", offset >= 0)
        assertEquals("`$word` is a name this file declares", MildaTypes.DECLARATION, tokenTypeAt(offset))
    }

    fun testTheNameKeepsItsColourWhenASpaceIsTypedBeforeTheColon() {
        myFixture.configureByText("a.milda", "foundations {\n  token white<caret>: color = hex(fff)\n}\n")
        myFixture.doHighlighting()
        assertDeclaration("white")

        myFixture.type(" ")
        assertDeclaration("white")

        // …and it survives taking the space back out again.
        myFixture.performEditorAction("EditorBackSpace")
        assertDeclaration("white")
    }

    fun testTheTypeAndTheLiteralPayloadSurviveAnEditToo() {
        myFixture.configureByText("a.milda", "foundations {\n  token white : color<caret> = hex(fff)\n}\n")
        myFixture.doHighlighting()
        val text = { myFixture.editor.document.text }
        assertEquals(MildaTypes.TYPE, tokenTypeAt(text().indexOf("color")))
        assertEquals(MildaTypes.NUMBER, tokenTypeAt(text().indexOf("fff")))

        myFixture.type(" ")
        assertEquals("the declared type is still a type", MildaTypes.TYPE, tokenTypeAt(text().indexOf("color")))
        assertEquals("and the payload is still the value", MildaTypes.NUMBER, tokenTypeAt(text().indexOf("fff")))
    }

    fun testEditingInsideAColourLiteralKeepsItsPayloadAValue() {
        myFixture.configureByText("a.milda", "foundations {\n  token white : color = hex(ff<caret>f)\n}\n")
        myFixture.doHighlighting()
        myFixture.type("a")
        assertEquals(
            "every word inside the parens is the literal's value, mid-edit as well",
            MildaTypes.NUMBER,
            tokenTypeAt(myFixture.editor.document.text.indexOf("ffaf")),
        )
    }

    fun testAScanResumedMidLineAgreesWithAScanFromTheTop() {
        // The invariant behind all of the above, checked directly: restarting at any token with
        // the state that token reported must reproduce the rest of the file exactly.
        val text = """
            foundations {
              token white : color = hex(fff)
              token pad : spacing = scaled(1.25)
            }
            component Badge "a label" {
              part root.label { ink color.text }
            }
        """.trimIndent()
        val whole = MildaHighlightScanner.scan(text)
        for ((at, span) in whole.withIndex()) {
            val resumed = MildaHighlightScanner.scan(text.subSequence(span.start, text.length), span.stateBefore)
            val expected = whole.drop(at).map { it.category }
            assertEquals("resuming at offset ${span.start}", expected, resumed.map { it.category })
        }
    }
}

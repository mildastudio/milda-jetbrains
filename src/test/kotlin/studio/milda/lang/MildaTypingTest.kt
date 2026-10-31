package studio.milda.lang

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

// The editing behaviour, driven through the platform's real typed-action handlers rather than
// asserted from the extension registrations: what a person typing in a `.milda` file gets.
class MildaTypingTest : BasePlatformTestCase() {
    fun testTypingAnOpeningBraceClosesIt() {
        myFixture.configureByText("a.milda", "foundations <caret>")
        myFixture.type('{')
        myFixture.checkResult("foundations {<caret>}")
    }

    fun testTypingAQuoteClosesIt() {
        myFixture.configureByText("a.milda", "component Button <caret>")
        myFixture.type('"')
        myFixture.checkResult("component Button \"<caret>\"")
    }

    fun testEnterInsideABlockIndentsToBraceDepth() {
        myFixture.configureByText("a.milda", "foundations {<caret>}")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
        // Two spaces: the Code Style default this language ships (MildaCodeStyleSettingsProvider).
        myFixture.checkResult("foundations {\n  <caret>\n}")
    }

    fun testEnterNestsOneLevelPerOpenBrace() {
        myFixture.configureByText("a.milda", "component B {\n  part root {<caret>}\n}\n")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
        myFixture.checkResult("component B {\n  part root {\n    <caret>\n  }\n}\n")
    }

    fun testEnterAtTheTopLevelStaysAtTheMargin() {
        // The file is a block with children too, and treating that like a brace group indented
        // every top-level declaration by one level. Nothing encloses a top-level statement.
        myFixture.configureByText("a.milda", "milda 0.1<caret>")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
        assertEquals("milda 0.1\n", myFixture.editor.document.text)

        myFixture.configureByText("a.milda", "foundations {\n  token a : color = hex(fff)\n}<caret>")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
        assertEquals(
            "a new declaration after a closed block starts at the margin",
            "foundations {\n  token a : color = hex(fff)\n}\n",
            myFixture.editor.document.text,
        )
    }

    fun testLineCommentUsesTheHashAndTogglesBack() {
        myFixture.configureByText("a.milda", "milda 0.1<caret>")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        // The document is what the action edits; the PSI is only re-committed on demand.
        assertEquals("# milda 0.1", myFixture.editor.document.text)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        assertEquals("uncomments back", "milda 0.1", myFixture.editor.document.text)
    }

    fun testBracesMatchAsAPair() {
        // What the brace matcher is for beyond auto-insert: the IDE can tell where a block ends.
        myFixture.configureByText("a.milda", "foundations {\n  token a : color = hex(fff)\n}\n")
        val text = myFixture.editor.document.charsSequence
        val highlighter = myFixture.editor.let { (it as com.intellij.openapi.editor.ex.EditorEx).highlighter }
        val iterator = highlighter.createIterator(text.indexOf('{'))
        assertEquals(MildaTypes.LBRACE, iterator.tokenType)
        assertTrue(
            "the pair is known to the platform",
            com.intellij.codeInsight.highlighting.BraceMatchingUtil.matchBrace(
                text,
                myFixture.file.fileType,
                iterator,
                true,
            ),
        )
    }
}

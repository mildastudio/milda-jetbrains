package studio.milda.lang

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.testFramework.fixtures.BasePlatformTestCase

// Landing in a block should ask the server what goes there.
//
// Reported twice, because the first fix only covered half of it: the server's trigger characters
// make the list open while you TYPE, but jumping into `component { … }` and typing nothing is not
// a keystroke the client will act on. Making the popup appear is the client's half, so it lives
// here - and the two failure modes are an EP name the platform does not have (registered, never
// called, no warning) and a predicate that fires in the wrong place (a popup inside a comment).
class MildaAutoPopupTest : BasePlatformTestCase() {
    private fun wants(src: String): Boolean {
        val caret = src.indexOf('▐')
        require(caret >= 0) { "the source needs a ▐ caret marker" }
        return MildaAutoPopup.wantsPopupAt(src.replace("▐", ""), caret)
    }

    fun testBothHandlersAreRegisteredWherePlatformWillCallThem() {
        assertTrue(
            "the typed handler must be on com.intellij.typedHandler",
            TypedHandlerDelegate.EP_NAME.extensionList.any { it is MildaTypedAutoPopup },
        )
        assertTrue(
            "the enter handler must be on com.intellij.enterHandlerDelegate",
            EnterHandlerDelegate.EP_NAME.extensionList.any { it is MildaEnterAutoPopup },
        )
    }

    fun testItOffersInsideABlockAndNowhereElse() {
        assertTrue("inside a component body", wants("component B {\n  ▐"))
        assertTrue("inside a nested block", wants("component B {\n  part root {\n    ▐"))
        assertTrue("right after the brace was typed", wants("foundations {▐}"))

        assertFalse("at the top of the file", wants("▐"))
        assertFalse("between two declarations", wants("foundations {\n}\n▐"))
        assertFalse("after every block is closed", wants("component B {\n  part root {\n  }\n}\n▐"))
    }

    fun testItStaysQuietInsideACommentOrAString() {
        // A `{` in either does not open a block, and a popup in either is noise.
        assertFalse("in a comment inside a block", wants("component B {\n  # what goes here? ▐"))
        assertFalse("in a description string", wants("component B \"a label ▐"))
        assertTrue("but the block itself still counts", wants("component B {\n  # a note\n  ▐"))
        // A brace inside a comment or a string is not a block, so it cannot make one.
        assertFalse("a brace in a comment opens nothing", wants("# component B {\n▐"))
        assertFalse("nor one in a string", wants("component B \"a { brace\"\n▐"))
    }

    fun testTypingTheOpeningBraceLeavesTheDocumentAlone() {
        // The handler only schedules a popup; the text must be exactly what the brace matcher
        // and the formatter would have produced on their own.
        myFixture.configureByText("a.milda", "foundations <caret>")
        myFixture.type("{")
        assertEquals("foundations {}", myFixture.editor.document.text)
    }
}

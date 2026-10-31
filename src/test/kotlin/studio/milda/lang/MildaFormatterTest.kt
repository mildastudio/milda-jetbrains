package studio.milda.lang

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.application.options.CodeStyle
import com.intellij.testFramework.fixtures.BasePlatformTestCase

// Reformat Code on a `.milda` file: fix the indentation, touch nothing else.
class MildaFormatterTest : BasePlatformTestCase() {
    private fun reformat(text: String): String {
        myFixture.configureByText("a.milda", text)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
        return myFixture.editor.document.text
    }

    fun testIndentsNestedBlocks() {
        assertEquals(
            "foundations {\n  token a : color = hex(fff)\n}\n",
            reformat("foundations {\ntoken a : color = hex(fff)\n}\n"),
        )
    }

    fun testIndentsOneLevelPerOpenBrace() {
        assertEquals(
            "component B {\n  part root {\n    fill accent.9\n  }\n}\n",
            reformat("component B {\npart root {\nfill accent.9\n}\n}\n"),
        )
    }

    fun testRemovesTooMuchIndentation() {
        assertEquals(
            "foundations {\n  token a : color = hex(fff)\n}\n",
            reformat("foundations {\n        token a : color = hex(fff)\n    }\n"),
        )
    }

    fun testKeepsColumnAlignmentInsideALine() {
        // The DSL's own examples line values up; a formatter that normalised spacing would
        // silently destroy that, so intra-line whitespace is left exactly as authored.
        val aligned =
            "foundations {\n  token brand   : color   = hex(3b82f6)\n  token space/2 : spacing = 8px\n}\n"
        assertEquals(aligned, reformat(aligned))
    }

    fun testKeepsBlankLinesBetweenDeclarations() {
        val spaced =
            "milda 0.1\n\ncomponent B {\n  archetype Button\n\n  part root {\n    fill accent.9\n  }\n}\n"
        assertEquals(spaced, reformat(spaced))
    }

    fun testFollowsTheConfiguredIndentSize() {
        myFixture.configureByText("a.milda", "foundations {\ntoken a : color = hex(fff)\n}\n")
        val indent = CodeStyle.getSettings(myFixture.file).getIndentOptions(MildaFileType)
        val previous = indent.INDENT_SIZE
        try {
            indent.INDENT_SIZE = 4
            myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
            assertEquals("foundations {\n    token a : color = hex(fff)\n}\n", myFixture.editor.document.text)
        } finally {
            indent.INDENT_SIZE = previous
        }
    }

    fun testLeavesAnAlreadyFormattedFileAlone() {
        val text = "milda 0.1\nprelude 0.0.2\n\ncontext-group Size { S1 S2 S3 }\n\n" +
            "foundations {\n  token accent/9 : color = hex(3b82f6)\n}\n\n" +
            "component Button \"Trigger an action\" {\n  archetype Button\n  props {\n" +
            "    size : Size = S2\n  }\n  part root {\n    layout { stack row; gap space.2 }\n" +
            "    fill accent.9\n    state hovered { fill accent.10 }\n  }\n}\n"
        assertEquals(text, reformat(text))
    }
}

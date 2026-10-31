package studio.milda.lang

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.testFramework.fixtures.BasePlatformTestCase

// *New | Milda File* has to be in the Project view's context menu, and the template behind it
// has to produce a file that actually parses.
//
// Two halves fail independently and silently: an action that is registered but anchored to a
// group id the platform does not have never appears, and a template whose Velocity the platform
// cannot render throws when the author picks it - after the dialog, when they have already
// named the file. So both are pinned here.
class MildaNewFileTest : BasePlatformTestCase() {
    fun testTheActionIsInTheNewGroup() {
        val actions = ActionManager.getInstance()
        val action = actions.getAction("Milda.NewFile")
        assertNotNull("the New | Milda File action must be registered", action)
        val newGroup = actions.getAction("NewGroup") as ActionGroup
        val inGroup = newGroup.getChildren(null).any { actions.getId(it) == "Milda.NewFile" }
        assertTrue("it must be a child of NewGroup, or it never shows in the New menu", inGroup)
    }

    fun testTheTemplateIsRegisteredUnderTheMildaExtension() {
        val template = FileTemplateManager.getInstance(project).getInternalTemplate(MildaCreateFileAction.TEMPLATE_NAME)
        // The extension comes from the template, so a name typed as `tokens.milda` cannot
        // produce `tokens.milda.milda`.
        assertEquals("milda", template.extension)
    }

    fun testTheTemplateRendersWithTheFileName() {
        val template = FileTemplateManager.getInstance(project).getInternalTemplate(MildaCreateFileAction.TEMPLATE_NAME)
        val text = template.getText(mapOf("NAME" to "Palette"))
        assertTrue("the file name reaches the document's own metadata", text.contains("name \"Palette\""))
        // Nothing left for Velocity to expand: an unrendered macro would land in the file as
        // literal text and the reader would take it for a token path.
        assertFalse("no macro survives rendering", text.contains("$"))
        // Balanced braces, per the same scanner the highlighter uses.
        val spans = MildaHighlightScanner.scan(text)
        val opens = spans.count { it.category == MildaHighlightScanner.LBRACE }
        val closes = spans.count { it.category == MildaHighlightScanner.RBRACE }
        assertEquals("the starter file's braces balance", opens, closes)
        assertTrue("and it declares foundations", text.contains("foundations {"))
    }
}

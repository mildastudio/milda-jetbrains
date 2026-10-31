package studio.milda.lang

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.options.colors.ColorSettingsPages
import com.intellij.testFramework.fixtures.BasePlatformTestCase

// *Settings | Editor | Color Scheme | Milda* is where the language's colours are named and
// changed, and it is also the only place a reader can DISCOVER that a token's type and a
// token's name are separate categories.
//
// Two things fail quietly here: a page that is not registered (nothing in the settings tree,
// no error anywhere) and a category that has no explicit colour, which is how a type name ended
// up the same colour as the plain text around it - the reason this page exists at all.
class MildaColorSettingsPageTest : BasePlatformTestCase() {
    private fun page(): ColorSettingsPage =
        ColorSettingsPages.getInstance().registeredPages.first { it.displayName == "Milda" }

    fun testThePageIsRegistered() {
        assertTrue("Milda must have a Color Scheme page", ColorSettingsPages.getInstance().registeredPages.any { it.displayName == "Milda" })
    }

    fun testEveryCategoryTheHighlighterUsesIsOnThePage() {
        val onPage = page().attributeDescriptors.map { it.key }.toSet()
        val used = listOf(
            MildaSyntaxHighlighter.KEYWORD, MildaSyntaxHighlighter.TYPE, MildaSyntaxHighlighter.DECLARATION,
            MildaSyntaxHighlighter.STRING, MildaSyntaxHighlighter.NUMBER, MildaSyntaxHighlighter.COMMENT,
            MildaSyntaxHighlighter.PROPERTY, MildaSyntaxHighlighter.VARIABLE, MildaSyntaxHighlighter.OPERATOR,
        )
        for (key in used) assertTrue("${key.externalName} must be on the page", key in onPage)
    }

    fun testTheTwoCategoriesWithoutAUsableFallbackShipTheirOwnColour() {
        // CLASS_NAME and IDENTIFIER are the plain foreground in most schemes, so these two
        // carry explicit colours (resources/colorSchemes/Milda*.xml). Without them the page
        // would list categories that all look the same.
        val scheme = EditorColorsManager.getInstance().globalScheme
        for (key in listOf(MildaSyntaxHighlighter.TYPE, MildaSyntaxHighlighter.DECLARATION)) {
            val attributes = scheme.getAttributes(key)
            assertNotNull("${key.externalName} has attributes", attributes)
            assertNotNull("${key.externalName} has a foreground of its own", attributes!!.foregroundColor)
        }
        assertFalse(
            "and the two are not the same colour",
            scheme.getAttributes(MildaSyntaxHighlighter.TYPE).foregroundColor ==
                scheme.getAttributes(MildaSyntaxHighlighter.DECLARATION).foregroundColor,
        )
    }

    fun testTheDemoTextIsRealMilda() {
        val demo = page().demoText
        assertTrue("shows a token declaration", demo.contains("token color/ink : color = hex("))
        assertTrue("shows a component", demo.contains("component Badge"))
        // Already formatted the way the formatter would format it, so the preview does not
        // silently show something other than what was written here.
        myFixture.configureByText("demo.milda", demo)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_REFORMAT)
        assertEquals("the demo is its own formatted form", demo, myFixture.editor.document.text)
    }
}

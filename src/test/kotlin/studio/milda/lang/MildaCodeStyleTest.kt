package studio.milda.lang

import com.intellij.application.options.CodeStyle
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

// *Settings | Editor | Code Style | Milda* has to actually be there.
//
// Registering a LanguageCodeStyleSettingsProvider is NOT enough, and the way it fails is
// silent: the platform decides which providers get a page by reflection, keeping only those
// that declare `createConfigurable` themselves rather than inheriting it
// (LanguageCodeStyleSettingsProvider.calcSettingPagesProviders). Inherit it, and the language
// still has working code style settings while no page exists to open - which is how this
// shipped at first, with nothing to see in the settings tree. Hence this test.
class MildaCodeStyleTest : BasePlatformTestCase() {
    fun testMildaHasAPageInTheCodeStyleTree() {
        val onPage = LanguageCodeStyleSettingsProvider.getSettingsPagesProviders().any { it.language === MildaLanguage }
        assertTrue("Milda must be among the languages with a Code Style page", onPage)
    }

    fun testThePageBuildsAndIsNamedMilda() {
        val provider = LanguageCodeStyleSettingsProvider.forLanguage(MildaLanguage)!!
        val configurable = provider.createConfigurable(CodeStyleSettings.getDefaults(), CodeStyleSettings.getDefaults())
        try {
            assertEquals("Milda", configurable.displayName)
            assertNotNull("the panel builds without a crash", configurable.createComponent())
        } finally {
            configurable.disposeUIResources()
        }
    }

    fun testShipsTheDslsOwnTwoSpaceDefault() {
        myFixture.configureByText("a.milda", "foundations {\n}\n")
        val indent = CodeStyle.getSettings(myFixture.file).getIndentOptions(MildaFileType)
        assertEquals(2, indent.INDENT_SIZE)
        assertEquals(2, indent.TAB_SIZE)
        assertFalse("spaces, not tabs", indent.USE_TAB_CHARACTER)
    }

    fun testTheCodeSampleShownInThePreviewIsRealMilda() {
        // The preview reformats the sample with the real formatter, so a sample that isn't
        // valid Milda would render as something no reader recognises.
        val sample = LanguageCodeStyleSettingsProvider.forLanguage(MildaLanguage)!!
            .getCodeSample(LanguageCodeStyleSettingsProvider.SettingsType.INDENT_SETTINGS)!!
        assertTrue("has a foundations block", sample.contains("foundations {"))
        assertTrue("has a component", sample.contains("component Button"))
        // And it is already formatted the way the formatter would format it.
        myFixture.configureByText("sample.milda", sample)
        myFixture.performEditorAction(com.intellij.openapi.actionSystem.IdeActions.ACTION_EDITOR_REFORMAT)
        assertEquals("the sample is its own formatted form", sample, myFixture.editor.document.text)
    }
}

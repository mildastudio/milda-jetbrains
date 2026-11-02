package studio.milda.lang

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

// Puts Milda in *Settings | Editor | Code Style* next to every other language, so the
// indentation of `.milda` files is a project setting a team can share (and .editorconfig
// can drive) rather than whatever each editor happens to do.
//
// Indentation is the whole of it: Milda's other layout choices aren't the IDE's to make -
// statements end at the newline, and blocks are brace-delimited with no wrapping or
// alignment rules to configure. Declaring only the indent options is what keeps the page
// honest: every tab shown does something. MildaLineIndentProvider is what applies them.
class MildaCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = MildaLanguage

    /**
     * Registering the provider is NOT what puts *Milda* in the Code Style tree. The platform
     * decides that by reflection: `LanguageCodeStyleSettingsProvider.calcSettingPagesProviders`
     * keeps a provider only when `createConfigurable` is declared by the provider itself and
     * not inherited from the base class. Inherit it and the language still HAS code style
     * settings (.editorconfig can drive them, the indent options are live) but there is no
     * page to open - which is exactly how this shipped at first.
     */
    override fun createConfigurable(
        baseSettings: CodeStyleSettings,
        modelSettings: CodeStyleSettings,
    ): CodeStyleConfigurable =
        object : CodeStyleAbstractConfigurable(baseSettings, modelSettings, configurableDisplayName) {
            override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
                object : TabbedLanguageCodeStylePanel(MildaLanguage, currentSettings, settings) {
                    // Only the tab that does something: see the class comment.
                    override fun initTabs(settings: CodeStyleSettings) = addIndentOptionsTab(settings)
                }
        }

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

    // Two spaces, matching the DSL's own examples and the walkthrough docs.
    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions,
    ) {
        indentOptions.INDENT_SIZE = 2
        indentOptions.CONTINUATION_INDENT_SIZE = 4
        indentOptions.TAB_SIZE = 2
        indentOptions.USE_TAB_CHARACTER = false
    }

    override fun getCodeSample(settingsType: SettingsType): String = SAMPLE

    private companion object {
        // The preview in the settings page. Kept to real, valid Milda covering the nesting
        // depths a reader cares about: a foundations token, and a component down to a
        // conditional facet inside a part.
        val SAMPLE = """
            milda 0.10
            prelude 0.0.2

            context-group Size { S1 S2 S3 }

            foundations {
              token accent/9 : color = hex(3b82f6)
              token space/2  : spacing = 8px
            }

            component Button "Trigger an action" {
              archetype Button
              props {
                size    : Size    = S2
                loading : boolean = false
              }
              events { click from A.invoke }
              part root {
                layout { stack row; gap space.2; align center }
                fill accent.9
                state hovered { fill accent.10 }
              }
            }
        """.trimIndent()
    }
}

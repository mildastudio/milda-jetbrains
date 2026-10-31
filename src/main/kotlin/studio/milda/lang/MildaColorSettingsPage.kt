package studio.milda.lang

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import studio.milda.MildaIcons
import javax.swing.Icon

/**
 * *Settings | Editor | Color Scheme | Milda* - the page that names every colour the language
 * uses and lets a reader change it.
 *
 * It exists for two reasons beyond configurability. It makes the categories DISCOVERABLE (the
 * only way to find out that a token's type and a token's name are distinct categories at all
 * is to see them listed), and it is where the plugin's own defaults become visible - the ones
 * in resources/colorSchemes, which the platform's plain fallbacks made invisible.
 */
class MildaColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = "Milda"

    override fun getIcon(): Icon = MildaIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = MildaSyntaxHighlighter()

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    /** No extra tags: every category in the sample is coloured by the lexer itself. */
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    // Every category, in a file that is valid Milda (MildaColorSettingsPageTest parses it).
    override fun getDemoText(): String = """
        # A design system, and every colour the language uses.
        context-group ColorScheme {
          Light
          Dark
        }

        foundations {
          token color/ink : color = hex(1a1a1a)
          token color/paper : color = oklch(0.98 0.01 250 / 90%)
          token space/2 : spacing = 8px

          token color/text : color = alias by ColorScheme {
            Light -> color.ink
            Dark -> color.paper
          }
        }

        component Badge "a small status label" {
          props { label : string }
          part root {
            kind container
            layout { stack row; gap space.2 }
            ink color.text
            content bind ${'$'}label
          }
        }
    """.trimIndent()

    private companion object {
        val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keyword", MildaSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("Type//Token type, colour and length literals", MildaSyntaxHighlighter.TYPE),
            AttributesDescriptor("Type//Declared name (token, component, part…)", MildaSyntaxHighlighter.DECLARATION),
            AttributesDescriptor("Value//Number, length, duration", MildaSyntaxHighlighter.NUMBER),
            AttributesDescriptor("Value//String", MildaSyntaxHighlighter.STRING),
            AttributesDescriptor("Reference//Token or machine path", MildaSyntaxHighlighter.PROPERTY),
            AttributesDescriptor("Reference//Prop reference", MildaSyntaxHighlighter.VARIABLE),
            AttributesDescriptor("Operator", MildaSyntaxHighlighter.OPERATOR),
            AttributesDescriptor("Comment", MildaSyntaxHighlighter.COMMENT),
        )
    }
}

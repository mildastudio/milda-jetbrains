package studio.milda.lang

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.ColorIcon
import java.awt.Color

// End-to-end check of the gutter swatch inside a real (headless) IDE: the daemon runs over a
// `.milda` file, the platform's ColorLineMarkerProvider asks MildaColorProvider, and a colour
// icon has to appear - exactly once per literal, whatever the colour space - with the picker's
// write-back landing in the document in the author's own notation.
class MildaColorGutterTest : BasePlatformTestCase() {
    private fun colorIcons(text: String): List<Color> {
        myFixture.configureByText("swatches.milda", text)
        myFixture.doHighlighting()
        return myFixture.findAllGutters()
            .mapNotNull { (it.icon as? ColorIcon)?.iconColor }
    }

    fun testOneSwatchPerLiteralInEverySpace() {
        val colors = colorIcons(
            """
            foundations {
              token brand : color = hex(3b82f6)
              token white : color = srgb(1 1 1)
              token vivid : color = oklch(0.62 0.19 256)
              token wide  : color = p3(1 0 0)
              token tint  : color = hsl(210 50% 40%)
              token space : spacing = 8px
            }
            """.trimIndent(),
        )
        assertEquals("one icon per colour literal, none for the spacing token", 5, colors.size)
        assertEquals(Color(0x3b, 0x82, 0xf6), colors[0])
        assertEquals(Color(0xff, 0xff, 0xff), colors[1])
        assertEquals(Color(0x33, 0x66, 0x99), colors[4])
    }

    fun testTheCssSpellingsGetASwatchToo() {
        // `rgba`/`hsla` are the same functions as `rgb`/`hsl`, commas are just separators, and a
        // 4-digit hex carries alpha. The reader accepts all of it, so the gutter must as well.
        val colors = colorIcons(
            """
            foundations {
              token a : color = rgba(59 130 246 0.5)
              token b : color = rgba(59, 130, 246, 0.5)
              token c : color = hsla(210 50% 40% / 0.5)
              token d : color = hex(f00c)
            }
            """.trimIndent(),
        )
        assertEquals("one icon per literal", 4, colors.size)
        assertEquals(Color(59, 130, 246, 128), colors[0])
        assertEquals("the comma form is the same colour", colors[0], colors[1])
        assertEquals(Color(0xff, 0x00, 0x00, 0xcc), colors[3])
    }

    fun testNoSwatchWhereThereIsNoColour() {
        val colors = colorIcons(
            """
            component Button "Trigger an action" {
              archetype Button
              props { size : Size = S2 }
              part root { fill accent.9 }
            }
            """.trimIndent(),
        )
        // A description string and a token reference are not colour literals. (A reference's
        // colour would mean resolving the token; the swatch is for literals only.)
        assertEquals(0, colors.size)
    }

    fun testPickingAColourRewritesTheLiteralInPlace() {
        myFixture.configureByText(
            "pick.milda",
            "foundations {\n  token vivid : color = oklch(0.62 0.19 256)\n}\n",
        )
        val element = myFixture.file.findElementAt(myFixture.file.text.indexOf("oklch"))!!
        val provider = MildaColorProvider()
        assertNotNull("the anchor carries the colour", provider.getColorFrom(element))

        WriteCommandAction.runWriteCommandAction(project) {
            provider.setColorTo(element, Color(0xff, 0x00, 0x00))
        }
        val text = myFixture.file.text
        assertTrue("stays an oklch literal: $text", text.contains("= oklch("))
        assertFalse("the old value is gone: $text", text.contains("0.19 256"))
        // And the rewritten literal reads back as the colour that was picked.
        val literal = Regex("oklch\\([^)]*\\)").find(text)!!.value
        assertEquals("#ff0000", MildaColor.toHex(MildaColor.parse(literal)!!))
    }

    fun testPickingAColourInTheRawHexEscapeKeepsItsQuotes() {
        myFixture.configureByText(
            "raw.milda",
            "foundations {\n  token legacy : color = raw \"#00000000\"\n}\n",
        )
        val element = myFixture.file.findElementAt(myFixture.file.text.indexOf("\"#0"))!!
        val provider = MildaColorProvider()
        WriteCommandAction.runWriteCommandAction(project) {
            provider.setColorTo(element, Color(0x00, 0x00, 0xff))
        }
        assertTrue(myFixture.file.text, myFixture.file.text.contains("raw \"#0000ff\""))
    }
}

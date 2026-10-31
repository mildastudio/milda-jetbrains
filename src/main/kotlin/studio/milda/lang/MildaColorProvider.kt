package studio.milda.lang

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ElementColorProvider
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import java.awt.Color

// The colour swatch in the gutter, and the colour picker behind it - what CSS, Java and
// Kotlin files already do, now for `.milda`.
//
// The platform does the UI: its own ColorLineMarkerProvider walks every element of every
// file and asks each ElementColorProvider for a colour, drawing the gutter icon and opening
// the picker for whatever answers. So all this class owes it is "what colour is this
// element, if any" and "write this colour back". The maths lives in MildaColor.
//
// Milda's PSI is deliberately flat (one element per highlight token - the LSP, not a grammar
// here, understands structure), so a literal like `oklch(0.62 0.19 256)` is SIX elements.
// The icon is anchored on the function name and the rest is read off the following siblings;
// anchoring anywhere else would draw one icon per token on the same line.
class MildaColorProvider : ElementColorProvider {
    override fun getColorFrom(element: PsiElement): Color? {
        // Cheap early out: these element types exist only in Milda files.
        val type = element.node?.elementType
        if (type !is MildaTokenType) return null

        if (type === MildaTypes.STRING) {
            // An escape value's raw colour: `token legacy : color = raw "#00000000"`.
            return MildaColor.parseHex(element.text)?.toAwt()
        }
        if (type !== MildaTypes.TYPE || element.text !in MildaColor.FUNCTIONS) return null
        val literal = literalAt(element) ?: return null
        return MildaColor.parse(element.text, literal.args)?.toAwt()
    }

    override fun setColorTo(element: PsiElement, color: Color) {
        val type = element.node?.elementType
        val picked = MildaColor.Srgb(
            color.red / 255.0,
            color.green / 255.0,
            color.blue / 255.0,
            color.alpha / 255.0,
        )
        val range = element.textRange
        when {
            type === MildaTypes.STRING ->
                // Keep the quotes: only the hex inside them is ours to change.
                rewrite(element, range.startOffset, range.endOffset, "\"${MildaColor.toHex(picked)}\"")
            type === MildaTypes.TYPE -> {
                val literal = literalAt(element) ?: return
                rewrite(element, range.startOffset, literal.end, MildaColor.format(element.text, picked))
            }
        }
    }

    /** The argument text of the `fn( … )` the anchor opens, and where the literal ends. */
    private class Literal(val args: String, val end: Int)

    private fun literalAt(anchor: PsiElement): Literal? {
        var sibling = anchor.nextSibling
        while (sibling is PsiWhiteSpace) sibling = sibling.nextSibling
        if (sibling?.node?.elementType !== MildaTypes.LPAREN) return null

        val args = StringBuilder()
        var cursor = sibling.nextSibling
        while (cursor != null) {
            val type = cursor.node?.elementType
            if (type === MildaTypes.RPAREN) return Literal(args.toString(), cursor.textRange.endOffset)
            // A nested paren, or a run past the end of the line, means this is not a colour
            // literal; better no swatch than a wrong one.
            if (type === MildaTypes.LPAREN || cursor.text.contains('\n')) return null
            args.append(cursor.text)
            cursor = cursor.nextSibling
        }
        return null // unterminated
    }

    private fun rewrite(element: PsiElement, start: Int, end: Int, replacement: String) {
        val project = element.project
        val file = element.containingFile ?: return
        val documents = PsiDocumentManager.getInstance(project)
        val document = documents.getDocument(file) ?: return
        // The platform calls this inside a write action but not inside a command, so the edit
        // would land in the undo stack unnamed; the command gives it a name and one Undo.
        WriteCommandAction.writeCommandAction(project, file)
            .withName("Change Colour")
            .run<RuntimeException> {
                document.replaceString(start, end, replacement)
                documents.commitDocument(document)
            }
    }

    private fun MildaColor.Srgb.toAwt(): Color = Color(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())
}

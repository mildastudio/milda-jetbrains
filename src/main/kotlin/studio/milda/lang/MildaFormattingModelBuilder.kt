package studio.milda.lang

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType

// What *Reformat Code* does to a `.milda` file, and what makes the Code Style page's preview
// react when you change the indent: one level of indentation per open brace, and nothing else.
//
// That "nothing else" is deliberate. Milda has no wrapping or alignment rules to apply -
// statements end at the newline - and its own examples line values up in columns
// (`token brand   : color   = hex(3b82f6)`). So every `getSpacing` here returns null, which
// tells the engine to leave the whitespace inside a line exactly as the author left it. A
// formatter that normalised spacing would silently destroy those columns.
//
// The block tree is built from the TOKENS rather than the PSI tree, because Milda's PSI is
// flat by design (the LSP understands structure, not this plugin): a `{` opens a group block
// whose contents get one normal indent and whose closing `}` gets none. That is the whole
// model, and it is the same rule MildaLineIndentProvider applies while you type.
class MildaFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val file = formattingContext.containingFile
        val tokens = file.node.getChildren(null).filter { it.elementType != TokenType.WHITE_SPACE }
        val root = MildaBlock(file.textRange, Indent.getNoneIndent(), group(tokens, 0, tokens.size))
        return FormattingModelProvider.createFormattingModelForPsiFile(file, root, formattingContext.codeStyleSettings)
    }

    /**
     * Blocks for `tokens[from, to)`: a `{` swallows everything up to its matching `}` into one
     * child block, so nesting in the block tree mirrors nesting in the file.
     */
    private fun group(tokens: List<ASTNode>, from: Int, to: Int): List<Block> {
        val blocks = ArrayList<Block>()
        var i = from
        while (i < to) {
            val node = tokens[i]
            if (node.elementType === MildaTypes.LBRACE) {
                val close = matchingBrace(tokens, i, to)
                val end = if (close < to) close else to - 1
                blocks.add(
                    MildaBlock(
                        TextRange(node.startOffset, tokens[end].textRange.endOffset),
                        Indent.getNoneIndent(),
                        braceBody(tokens, i, close, to),
                        // A brace group is the ONE thing that indents what goes inside it.
                        indentsChildren = true,
                    ),
                )
                i = end + 1
                continue
            }
            blocks.add(MildaBlock(node.textRange, Indent.getNoneIndent(), emptyList()))
            i++
        }
        return blocks
    }

    /** The `{`, then the body one indent deeper, then the `}` back at the block's own level. */
    private fun braceBody(tokens: List<ASTNode>, open: Int, close: Int, to: Int): List<Block> {
        val blocks = ArrayList<Block>()
        blocks.add(MildaBlock(tokens[open].textRange, Indent.getNoneIndent(), emptyList()))
        val bodyEnd = if (close < to) close else to
        for (block in group(tokens, open + 1, bodyEnd)) {
            blocks.add(IndentedBlock(block))
        }
        if (close < to) blocks.add(MildaBlock(tokens[close].textRange, Indent.getNoneIndent(), emptyList()))
        return blocks
    }

    private fun matchingBrace(tokens: List<ASTNode>, open: Int, to: Int): Int {
        var depth = 0
        for (i in open until to) {
            when (tokens[i].elementType) {
                MildaTypes.LBRACE -> depth++
                MildaTypes.RBRACE -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return to // unclosed while typing: treat the rest of the file as the body
    }

    private open class MildaBlock(
        private val range: TextRange,
        private val ownIndent: Indent,
        private val children: List<Block>,
        /** Whether a line STARTED inside this block belongs one level deeper. True only for a
         *  brace group: the file itself is a block with children too, and treating that the
         *  same way indented every top-level declaration by one level - which is what pressing
         *  Enter at the top of a file used to do. */
        private val indentsChildren: Boolean = false,
    ) : Block {
        override fun getTextRange(): TextRange = range
        override fun getSubBlocks(): List<Block> = children
        override fun getWrap(): Wrap? = null
        override fun getIndent(): Indent = ownIndent
        override fun getAlignment(): Alignment? = null

        // null = "no requirement": keep the author's own spacing inside a line, and their
        // blank lines between declarations. Indentation is applied independently of this.
        override fun getSpacing(child1: Block?, child2: Block): Spacing? = null

        // Where the caret goes on Enter inside this block - one level in, for a brace group.
        override fun getChildAttributes(newChildIndex: Int): ChildAttributes =
            ChildAttributes(if (indentsChildren) Indent.getNormalIndent() else Indent.getNoneIndent(), null)

        override fun isIncomplete(): Boolean = false
        override fun isLeaf(): Boolean = children.isEmpty()
    }

    /** Same block, one indent level deeper: what the body of a brace group gets. */
    private class IndentedBlock(private val delegate: Block) : Block {
        override fun getTextRange(): TextRange = delegate.textRange
        override fun getSubBlocks(): List<Block> = delegate.subBlocks
        override fun getWrap(): Wrap? = delegate.wrap
        override fun getIndent(): Indent = Indent.getNormalIndent()
        override fun getAlignment(): Alignment? = delegate.alignment
        override fun getSpacing(child1: Block?, child2: Block): Spacing? = delegate.getSpacing(child1, child2)
        override fun getChildAttributes(newChildIndex: Int): ChildAttributes = delegate.getChildAttributes(newChildIndex)
        override fun isIncomplete(): Boolean = delegate.isIncomplete
        override fun isLeaf(): Boolean = delegate.isLeaf
    }
}

package studio.milda.lang

import com.intellij.lang.Commenter

// Cmd/Ctrl+/ on `.milda`. The DSL has line comments only (`# …`), so the block-comment
// half of the contract is deliberately null - the platform then greys out "Comment with
// Block Comment" instead of inserting a delimiter the parser would choke on.
class MildaCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "# "
    override fun getBlockCommentPrefix(): String? = null
    override fun getBlockCommentSuffix(): String? = null
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}

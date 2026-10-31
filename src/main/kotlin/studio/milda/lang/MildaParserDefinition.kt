package studio.milda.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class MildaFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MildaLanguage) {
    override fun getFileType() = MildaFileType
}

// A flat PSI over the highlight lexer's tokens. No comment/string token sets are
// declared, so the built-in spellchecker has nothing to check on `.milda` files - no
// "Typo" noise on hex colors / identifiers. (Highlighting is separate, via the
// SyntaxHighlighterFactory.)
class MildaParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?) = MildaHighlightLexer()

    override fun createParser(project: Project?) = PsiParser { root, builder ->
        val mark = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        mark.done(root)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = MildaTypes.FILE
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = MildaFile(viewProvider)
}

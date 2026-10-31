package studio.milda.lang

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Open the completion list when the caret ARRIVES in a block, without waiting for a keystroke.
 *
 * The language server can say which words belong where, but it cannot make an editor show them:
 * a popup opens either because the author asked for it or because the typed character was one of
 * the server's `triggerCharacters`. Type `{`, press Enter, and you are inside `component { … }`
 * having typed nothing that qualifies - so the editor sat silent exactly where a reader has the
 * most to ask ("what goes in here?"). That gap is the client's to close, which means ours.
 *
 * Deliberately narrow: only inside a block, and never inside a comment or a string. A popup at
 * the top of the file on every Enter would be noise, and the language's blocks are where the
 * vocabulary is closed enough to be worth offering.
 */
object MildaAutoPopup {
    /**
     * Whether an offer makes sense at `offset`: the caret is inside at least one open brace and
     * not inside a comment or a string literal.
     *
     * Brace depth comes from the scanner rather than a character count, so a `{` inside a comment
     * or a description string does not open a block (the same reason MildaBraceMatcher works on
     * token types).
     */
    fun wantsPopupAt(text: CharSequence, offset: Int): Boolean {
        if (offset <= 0 || offset > text.length) return false
        val spans = MildaHighlightScanner.scan(text.subSequence(0, offset))
        val last = spans.lastOrNull()
        // A comment or an unterminated string running to the caret means the caret is in it.
        if (last != null && last.end >= offset &&
            (last.category == MildaHighlightScanner.COMMENT || last.category == MildaHighlightScanner.STRING)
        ) {
            return false
        }
        var depth = 0
        for (span in spans) {
            when (span.category) {
                MildaHighlightScanner.LBRACE -> depth++
                MildaHighlightScanner.RBRACE -> if (depth > 0) depth--
            }
        }
        return depth > 0
    }

    fun schedule(project: Project, editor: Editor) {
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
    }
}

/** Typing the `{` that opens a block: the platform inserts the `}` (MildaBraceMatcher), and the
 *  list of what may go between them opens with it. */
class MildaTypedAutoPopup : TypedHandlerDelegate() {
    override fun charTyped(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (charTyped == '{' && file.fileType === MildaFileType &&
            MildaAutoPopup.wantsPopupAt(editor.document.charsSequence, editor.caretModel.offset)
        ) {
            MildaAutoPopup.schedule(project, editor)
        }
        return Result.CONTINUE
    }
}

/** Enter inside a block - the other way a caret lands on an empty line with a question. */
class MildaEnterAutoPopup : EnterHandlerDelegateAdapter() {
    override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): EnterHandlerDelegate.Result {
        if (file.fileType === MildaFileType &&
            MildaAutoPopup.wantsPopupAt(editor.document.charsSequence, editor.caretModel.offset)
        ) {
            MildaAutoPopup.schedule(file.project, editor)
        }
        return EnterHandlerDelegate.Result.Continue
    }
}

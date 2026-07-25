package studio.milda.lang

import com.intellij.lang.Language

// A native language for `.milda`: no real grammar (the LSP does the parsing/intelligence),
// but a lexer-based syntax highlighter (kept in parity with the LSP's TS classifier) plus
// a trivial parser. This gives native, offline highlighting, a recognized file type (icon,
// spellcheck control, plugin-advertiser), while the LSP layers diagnostics/completion/etc.
object MildaLanguage : Language("Milda")

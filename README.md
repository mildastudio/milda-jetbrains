# Milda for JetBrains

JetBrains IDE support for the [Milda](https://github.com/mildastudio/milda-lang)
design-system DSL (`.milda`).

- **Works out of the box:** syntax highlighting, file recognition + icon, and code
  commenting - no external tools required.
- **With the language server** ([`@mildastudio/milda-lsp`](https://github.com/mildastudio/milda-lsp),
  a Node.js process the plugin launches and bundles): diagnostics, completion, hover,
  go-to-definition, document outline, and folding.

The language-server features use the IntelliJ LSP API, available only in **paid** JetBrains
IDEs (IntelliJ IDEA Ultimate, WebStorm, PyCharm, GoLand, Rider, …), not Community editions.

## Install

From the JetBrains Marketplace: **Settings → Plugins → Marketplace → search "Milda"**.
You only need **Node.js** on your machine - the language server ships inside the plugin.

## Build from source

Needs a JDK to run Gradle (the wrapper is pinned; the toolchain auto-provisions JDK 21)
and Node.js to fetch the bundled server:

```bash
npm install @mildastudio/milda-lsp   # the server the plugin bundles
./gradlew buildPlugin                 # → build/distributions/*.zip
```

Install the zip via **Settings → Plugins → ⚙ → Install Plugin from Disk…**, or run a
sandbox IDE with `./gradlew runIde`.

## Highlighter ↔ server parity

The native syntax highlighter is kept byte-for-byte in step with the language server's
classifier via a shared corpus and golden tests - see the
[`@mildastudio/milda-lsp`](https://github.com/mildastudio/milda-lsp) repo.

## License

MIT © Péter Viola

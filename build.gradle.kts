// Milda JetBrains plugin - thin launcher for the @mildastudio/milda-lsp language
// server via the IntelliJ Platform LSP API.
//
// The LSP API (com.intellij.platform.lsp) ships ONLY in paid JetBrains IDEs, so this
// module builds against IntelliJ IDEA Ultimate. The plugin itself is tiny; all the
// language intelligence lives in the Node server.

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    // Match the Kotlin the target IDE ships (WebStorm/IntelliJ 2026.1 → Kotlin 2.3.x),
    // otherwise the compiler can't read the platform classes' metadata.
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "studio.milda"
// Release automation passes the version from the tag: `-PpluginVersion=1.2.3`.
version = providers.gradleProperty("pluginVersion").getOrElse("0.1.0")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Build against a locally installed paid IDE when a path is provided (fast, no
// download, and matches the exact IDE you'll run the plugin in), otherwise download
// IntelliJ IDEA Ultimate. Point at a local IDE with either:
//   ./gradlew buildPlugin -PmildaIdePath=/Users/you/Applications/WebStorm.app
//   MILDA_IDE_PATH=/Applications/WebStorm.app ./gradlew buildPlugin
val localIdePath: String? =
    providers.gradleProperty("mildaIdePath").orNull
        ?: providers.environmentVariable("MILDA_IDE_PATH").orNull

dependencies {
    intellijPlatform {
        if (localIdePath != null) {
            local(localIdePath)
        } else {
            // Ultimate bundles the LSP API module. 2026.1 is the floor the sources need: the
            // descriptor turns the platform's semantic-token layer off through
            // `LspCustomization`, which 2024.2's LSP API does not have yet, so a 2024.2
            // target compiled nowhere but against a local 2026.1 IDE (red mirror CI from
            // 2026-07-25 to 2026-11-02).
            intellijIdeaUltimate("2026.1")
        }

        // If the compiler can't resolve `com.intellij.platform.lsp.api.*`, uncomment:
        // bundledModule("com.intellij.modules.lsp")

        // Platform test framework (JUnit4 + platform classes) for the lexer parity test.
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 261 = 2026.1, the platform the plugin compiles against (see `dependencies`).
            // The LSP API itself goes back to 2023.2, but `LspCustomization` does not, and a
            // lower floor would install the plugin where its LSP half cannot load. Open-ended
            // upper bound so the plugin keeps working on future IDEs (highlighting is native;
            // the customization hook still resolves on 2026.2).
            sinceBuild = "261"
            untilBuild = provider { null }
        }
    }

    // Marketplace requires signed plugins. Secrets are supplied by the release workflow
    // as env vars (never committed); locally these are simply absent and signing is a no-op.
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    // `./gradlew publishPlugin` with a Marketplace token (from the release workflow).
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    // `./gradlew verifyPlugin` runs the JetBrains Plugin Verifier. Target paid IDEs only -
    // the plugin depends on the LSP module (com.intellij.modules.lsp), absent in Community.
    pluginVerification {
        ides {
            recommended()
        }
    }
}

// WebStorm / IntelliJ 2026.1 run on JBR 21. The foojay resolver (settings.gradle.kts)
// auto-downloads a JDK 21 for this toolchain if the host doesn't have one.
kotlin {
    jvmToolchain(21)
}

tasks {
    // We ship no searchable settings UI worth indexing; skipping this avoids launching
    // the IDE headlessly during the build (much faster, fewer moving parts).
    buildSearchableOptions {
        enabled = false
    }

    // The tests open `.milda` files in a real editor, and on open the platform instantiates
    // EVERY registered LSP provider, not just ours. In 2026.1 the bundled Vue plugin's
    // provider dies in a static-initialisation cycle of its own (`VueLspServerActivationRule`
    // <-> `VueLspServerLoader`), and the test framework turns that logged error into a
    // failure of whichever test was running - which it did on the Linux runner and not on a
    // Mac, so it read as flaky. Vue is not under test; keep it out of the test IDE.
    prepareTestSandbox {
        disabledPlugins.add("org.jetbrains.plugins.vue")
    }
}

// Bundle the built language server into the plugin, so users only need Node.js on their
// machine - no `npm install` of the server. The server dist is sourced from either the
// monorepo sibling (dev) or a `npm i @mildastudio/milda-lsp` install (standalone public
// repo / release CI).
val serverDistCandidates = listOf(
    layout.projectDirectory.file("../milda-lsp/dist/index.cjs"), // monorepo
    layout.projectDirectory.file("node_modules/@mildastudio/milda-lsp/dist/index.cjs"), // standalone
)
val serverDist = serverDistCandidates.firstOrNull { it.asFile.exists() }
val bundleServer by tasks.registering(Copy::class) {
    doFirst {
        if (serverDist == null) {
            throw GradleException(
                "Language server not found. In the monorepo: `npm run build -w @mildastudio/milda-lsp`. " +
                    "Standalone: `npm i @mildastudio/milda-lsp`.",
            )
        }
    }
    if (serverDist != null) from(serverDist)
    into(layout.projectDirectory.dir("src/main/resources/server"))
    rename { "milda-lsp.cjs" }
}
tasks.named("processResources") { dependsOn(bundleServer) }

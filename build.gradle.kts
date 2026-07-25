// Milda JetBrains plugin — thin launcher for the @mildastudio/milda-lsp language
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
            // Any paid IDE >= 2023.2 works; Ultimate bundles the LSP API module.
            intellijIdeaUltimate("2024.2")
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
            // 2023.2 is the first build with the LSP API; open-ended upper bound so the
            // plugin keeps working on future IDEs (highlighting is native; LSP is stable).
            sinceBuild = "232"
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

    // `./gradlew verifyPlugin` runs the JetBrains Plugin Verifier. Target paid IDEs only —
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
}

// Bundle the built language server into the plugin, so users only need Node.js on their
// machine — no `npm install` of the server. The server dist is sourced from either the
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

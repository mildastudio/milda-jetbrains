// Auto-provisions the JDK the toolchain requests (build.gradle.kts `jvmToolchain`), so
// the build works regardless of which JDK the developer has installed. 1.0.0 is the
// first release compatible with Gradle 9.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "milda-jetbrains"

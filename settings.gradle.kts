pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }

    plugins {
        val kotlinVersion = extra["kotlin.version"] as String

        kotlin("multiplatform").version(kotlinVersion)
    }
}

rootProject.name = "ldests"

include(":shared")
include(":jsLibrary")

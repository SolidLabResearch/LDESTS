@file:Suppress("OPT_IN_IS_NOT_ENABLED")

import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

version = "1.0-SNAPSHOT"
val ktorVersion = extra["ktor.version"]

tasks.withType<KotlinJsCompile>().configureEach {
    kotlinOptions.moduleKind = "commonjs"
}

kotlin {
//    jvm("desktop")

    js(IR) {
        nodejs {
            useCommonJs()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
//                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
                // resolves https://youtrack.jetbrains.com/issue/KT-57235
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-RC")
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation(npm("@comunica/query-sparql", "2.6.9"))
            }
        }

//        val desktopMain by getting {
//            dependencies {
//                implementation(compose.desktop.common)
//                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
//                implementation("io.ktor:ktor-client-cio:$ktorVersion")
//            }
//        }
    }
}

// Use a proper version of webpack, TODO remove after updating to Kotlin 1.9.
rootProject.the<NodeJsRootExtension>().versions.webpack.version = "5.76.2"

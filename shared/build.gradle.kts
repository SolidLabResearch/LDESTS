import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

plugins {
    kotlin("multiplatform")
}

version = "1.0-SNAPSHOT"

tasks.withType<KotlinJsCompile>().configureEach {
    kotlinOptions.moduleKind = "commonjs"
}

kotlin {

    js(IR) {
        nodejs {
            useCommonJs()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // resolves https://youtrack.jetbrains.com/issue/KT-57235
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-RC")
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(npm("@comunica/query-sparql", "2.6.9"))
            }
        }

    }
}

// Use a proper version of webpack, TODO remove after updating to Kotlin 1.9.
rootProject.the<NodeJsRootExtension>().versions.webpack.version = "5.76.2"

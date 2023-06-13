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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(npm("@comunica/query-sparql", "2.6.9"))
                implementation(npm("n3", "1.16.4"))

                val util = projectDir.resolve("src/jsMain/js/build/base-1.0.0.tgz").canonicalFile
                implementation(npm("base", "file:$util"))

            }
        }

    }
}

afterEvaluate {
    // task to build the JS base code
    val buildBaseUtilTask = tasks.create("buildBaseUtilTask", Exec::class.java) {
        workingDir = File("src/jsMain/js/src")
        commandLine = listOf("npm", "pack", "--pack-destination", "../build")
        doFirst { mkdir("${workingDir.parent}/build") }
    }
    project.tasks.getByName("build").dependsOn(buildBaseUtilTask)
}

// Use a proper version of webpack, TODO remove after updating to Kotlin 1.9.
rootProject.the<NodeJsRootExtension>().versions.webpack.version = "5.76.2"

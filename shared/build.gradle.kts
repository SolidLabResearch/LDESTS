import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.incremental.createDirectory

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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
                // requires "@js-joda/core" for jsMain
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(npm("@comunica/query-sparql", "2.6.9"))
                implementation(npm("n3", "1.16.4"))

                val util = projectDir.resolve("src/jsMain/js/build/base-1.0.0.tgz").canonicalFile
                implementation(npm("base", "file:$util"))

                val incremunica = projectDir.resolve("src/jsMain/build/comunica-query-sparql-incremental-1.0.0.tgz").canonicalFile
                implementation(npm("@comunica/query-sparql-incremental", "file:$incremunica"))

                val streamingStore = projectDir.resolve("src/jsMain/build/comunica-incremental-rdf-streaming-store-1.0.0.tgz").canonicalFile
                implementation(npm("@comunica/incremental-rdf-streaming-store", "file:$streamingStore"))

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
    // task to integrate incremunica
    val configureIncremunicaEngine = tasks.create("configureIncremunicaEngine", Exec::class.java) {
        val build = File("${workingDir.absolutePath}/" + File("src/jsMain/build"))
        if (!File("$build/incremunica/").exists()) {
            exec {
                workingDir = build
                workingDir.createDirectory()
                commandLine("git", "clone", "https://github.com/maartyman/incremunica.git")
            }
            exec {
                workingDir = File("$build/incremunica")
                commandLine("yarn", "install")
            }
        }
        workingDir = File("$build/incremunica/engines/query-sparql-incremental")
        commandLine("npm", "pack", "--pack-destination", build)
    }
    project.tasks.getByName("build").dependsOn(configureIncremunicaEngine)
    val configureIncremunicaStreamingStore = tasks.create("configureIncremunicaStreamingStore", Exec::class.java) {
        val build = File("${workingDir.absolutePath}/" + File("src/jsMain/build"))
        if (!File("$build/incremunica/").exists()) {
            exec {
                workingDir = build
                workingDir.createDirectory()
                commandLine("git", "clone", "https://github.com/maartyman/incremunica.git")
            }
            exec {
                workingDir = File("$build/incremunica")
                commandLine("yarn", "install")
            }
        }
        workingDir = File("$build/incremunica/packages/incremental-rdf-streaming-store")
        commandLine("npm", "pack", "--pack-destination", build)
    }
    project.tasks.getByName("build").dependsOn(configureIncremunicaEngine)
    project.tasks.getByName("build").dependsOn(configureIncremunicaStreamingStore)
}

// Use a proper version of webpack, TODO remove after updating to Kotlin 1.9.
rootProject.the<NodeJsRootExtension>().versions.webpack.version = "5.76.2"

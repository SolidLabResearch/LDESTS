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
                val util = projectDir.resolve("src/jsMain/js/build/ldests_compat-1.0.0.tgz").canonicalFile
                implementation(npm("ldests_compat", "file:$util"))
            }
        }

    }
}

afterEvaluate {
    // task to build the JS base code
    val buildJsCompatTask = tasks.create("buildJsCompatTask", Exec::class.java) {
        workingDir = File("src/jsMain/js")
        commandLine = listOf("npm", "pack", "--pack-destination", "./build")
        doFirst { mkdir("${workingDir.parent}/build") }
    }
    project.tasks.getByName("compileKotlinJs").dependsOn(buildJsCompatTask)
    // task to integrate incremunica
    val configureIncremunicaEngine = tasks.create("configureIncremunicaEngine", Exec::class.java) {
        val lib = File("${workingDir.absolutePath}/" + File("src/jsMain/js/lib"))
        if (!File("$lib/incremunica/").exists()) {
            buildJsCompatTask.dependsOn(this)
            exec {
                workingDir = lib
                workingDir.createDirectory()
                commandLine("git", "clone", "https://github.com/maartyman/incremunica.git")
            }
        }
        workingDir = File("$lib/incremunica")
        commandLine("yarn", "install")
    }
}

// Use a proper version of webpack, TODO remove after updating to Kotlin 1.9.
rootProject.the<NodeJsRootExtension>().versions.webpack.version = "5.76.2"

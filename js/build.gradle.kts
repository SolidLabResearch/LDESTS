plugins {
    kotlin("multiplatform")
}

afterEvaluate {
    release()
    debug()
    test()
}

kotlin {
    js(IR) {
        moduleName = "ldests"
        generateTypeScriptDefinitions()
        nodejs()
        binaries.library()
    }
    sourceSets {
        all {
            languageSettings.optIn("kotlin.js.ExperimentalJsExport")
            languageSettings.optIn("ExternalUse")
        }
        val jsMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.7.2")
            }
        }
    }
}

fun release() {
    // "post-processing" the resulting JS/TS build
    val jsLibRelease = tasks.create("release", Task::class.java) {
        doLast {
            // 0: setting up folders/locations
            val root = project.rootDir
            val build = "$root/build/js/packages/ldests/"
            val output = "$root/bin/js"
            // 1: copying the general README, so it is included in the installed pack
            copy {
                from("$root/README.md")
                into("$root/build/js/packages/ldests/")
            }
            // 2: fixing the bindings for manual imports from external dependencies
            File("$root/build/js/packages/ldests/kotlin/ldests.d.ts")
                .appendText("\nimport { Triple, NamedNode } from \"n3\";\nimport { Bindings as ComunicaBinding } from \"@incremunica/incremental-types\"")
            // 3: erasing the source maps for export reasons
            delete {
                val maps = File("$build/kotlin")
                    .listFiles()
                    ?.filter { it.extension == "map" }
                    ?: emptyList<String>()
                delete(maps)
            }
            // 4: packing it together & exporting
            mkdir(output)
            exec {
                workingDir = File(build)
                commandLine("npm", "pack", "--pack-destination", output)
            }
        }
    }
    val compileTask = tasks.getByName("build")
    // always require a build to happen first
    jsLibRelease.dependsOn(compileTask)
}

fun debug() {
    // "post-processing" the resulting JS/TS build
    val jsLibDebug = tasks.create("debug", Task::class.java) {
        doLast {
            // 0: setting up folders/locations
            val root = project.rootDir
            val build = "$root/build/js/packages/ldests/"
            val output = "$root/bin/js"
            // 1: copying the general README, so it is included in the installed pack
            copy {
                from("$root/README.md")
                into("$root/build/js/packages/ldests/")
            }
            // 2: fixing the bindings for manual imports from external dependencies
            File("$root/build/js/packages/ldests/kotlin/ldests.d.ts")
                .appendText("\nimport { Triple, NamedNode } from \"n3\";\nimport { Bindings as ComunicaBinding } from \"@incremunica/incremental-types\"")
            // 3: keeping the source maps so the test can better show where it goes wrong
            // 4: packing it together & exporting
            mkdir(output)
            exec {
                workingDir = File(build)
                commandLine("npm", "pack", "--pack-destination", output)
            }
        }
    }
    val compileTask = tasks.getByName("build")
    // always require a build to happen first
    jsLibDebug.dependsOn(compileTask)
}

fun test() {
    // task to link the resulting generated code to the example app's modules
    val root = project.rootDir
    val install = tasks.create("install", Exec::class.java) {
        workingDir = File("$root/jsTest")
        commandLine = listOf("npm", "i", "file:$root/bin/js/ldests-$version.tgz")
    }
    // task to test the library by running the `exampleJsApp`
    val run = tasks.create("run", Exec::class.java) {
        workingDir = root
        commandLine = listOf("npx", "ts-node", "jsTest/index.ts")
    }
    // configuring these tasks to require the build to be made, but not necessarily after every build
    install.dependsOn(tasks.getByName("debug"))
    // configuring the test to require this update to happen first
    run.dependsOn(install)
}

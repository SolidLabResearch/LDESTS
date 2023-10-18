plugins {
    kotlin("multiplatform")
}

//val copyJsResources = tasks.create("copyJsResourcesWorkaround", Copy::class.java) {
//    from(project(":shared").file("src/commonMain/resources"))
//    into("build/processedResources/js/main")
//}

//afterEvaluate {
//    project.tasks.getByName("jsProcessResources").finalizedBy(copyJsResources)
//    copyJsResources.dependsOn(project.tasks.getByName("jsDevelopmentLibraryCompileSync"))
//    copyJsResources.dependsOn(project.tasks.getByName("jsProductionLibraryCompileSync"))
//
////    project.tasks.getByName("jsNodeRun").dependsOn(project.tasks.getByName("jsDevelopmentLibraryCompileSync"))
//    project.tasks.getByName("jsNodeRun").dependsOn(project.tasks.getByName("jsProductionLibraryCompileSync"))
//}

afterEvaluate {
    export()
    test()
}

kotlin {
    js(IR) {
        moduleName = "ldests"
        generateTypeScriptDefinitions()
        nodejs {
//            useCommonJs()
        }
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

fun export() {
    // "post-processing" the resulting JS/TS build
    val root = project.rootDir
    val jsBuild = tasks.create("jsBuild", Copy::class.java) {
        doFirst { mkdir("$root/bin/js") }
        from("$root/build/js/packages/ldests")
        into("$root/bin/js")
        // fix the created build's typescript definitions, which is currently still missing the import statement
        //  of the referenced external types
        doLast {
            exec {
                workingDir = File("$root/bin/js")
                commandLine = listOf("npm", "i")
            }
            File("$root/bin/js/kotlin/ldests.d.ts")
                .appendText("\nimport { Triple, NamedNode } from \"n3\";\nimport { Bindings as ComunicaBinding } from \"@incremunica/incremental-types\"")
        }
    }
    val build = tasks.getByName("build")
    // always require a build to happen first
    jsBuild.dependsOn(build)
}

fun test() {
    // task to link the resulting generated code to the example app's modules
    val root = project.rootDir
    val jsUpdateTask = tasks.create("jsUpdateTask", Exec::class.java) {
        workingDir = File("$root/jsTest")
        commandLine = listOf("npm", "i", "file:$root/bin/js")
    }
    // task to test the library by running the `exampleJsApp`
    val jsTestTask = tasks.create("jsTestTask", Exec::class.java) {
        workingDir = root
        commandLine = listOf("npx", "ts-node", "jsTest/index.ts")
    }
    // configuring these tasks to require the build to be made, but not necessarily after every build
    jsUpdateTask.dependsOn(project.tasks.getByName("jsBuild"))
    // configuring the test to require this update to happen first
    jsTestTask.dependsOn(jsUpdateTask)
}

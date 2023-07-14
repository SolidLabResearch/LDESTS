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
    createPostprocessingTasks()
    createTestTask()
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

fun createPostprocessingTasks() {
    // "post-processing" the resulting JS/TS build
    val root = project.rootDir
    val jsBuild = tasks.create("jsBuild", Copy::class.java) {
        doFirst { mkdir("$root/bin/js") }
        from("$root/build/js/packages/ldests")
        into("$root/bin/js")
    }
    val build = tasks.getByName("build")
    // always require a build to happen first
    jsBuild.dependsOn(build)
    // also adding the compatibility layer, if it does not exist yet
    if (!File("$root/bin/js/node_modules/ldests_compat").exists()) {
        val jsFinalize = tasks.create("jsFinalize", Copy::class.java) {
            doFirst { mkdir("$root/bin/js/node_modules/ldests_compat") }
            from("$root/shared/src/jsMain/js")
            into("$root/bin/js/node_modules/ldests_compat")
        }
        // always finalize the bin result after creating one, but always creating one first
        jsBuild.finalizedBy(jsFinalize)
        jsFinalize.dependsOn(jsBuild)
    }
}

fun createTestTask() {
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

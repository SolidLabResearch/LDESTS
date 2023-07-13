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
    // task to copy the resulting generated code to the example app's modules
    val updateExampleApp = tasks.create("updateExampleApp", Copy::class.java) {
        doFirst { mkdir("../exampleJsApp/node_modules/ldests") }
        from("../build/js/packages/ldests")
        into("../exampleJsApp/node_modules/ldests")
    }
    // task to update the exampleJsApp with this base code
    val updateExampleAppCompat = tasks.create("updateExampleAppCompat", Copy::class.java) {
        doFirst { mkdir("../exampleJsApp/node_modules/ldests/node_modules/ldests_compat") }
        from("../shared/src/jsMain/js")
        into("../exampleJsApp/node_modules/ldests/node_modules/ldests_compat")
    }
    updateExampleApp.finalizedBy(updateExampleAppCompat)
    // task to test the library by running the `exampleJsApp`
    val testLibraryTask = tasks.create("testLibraryTask", Exec::class.java) {
        workingDir = File("..")
        commandLine = listOf("npx", "ts-node", "exampleJsApp/index.ts")
    }
    project.tasks.getByName("build").finalizedBy(updateExampleApp)
    updateExampleApp.dependsOn(project.tasks.getByName("build"))

    // configuring the test to run as well
    updateExampleApp.finalizedBy(testLibraryTask)
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

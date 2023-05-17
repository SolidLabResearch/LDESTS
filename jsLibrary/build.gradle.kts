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

// task to copy the resulting generated code to the example app's modules
afterEvaluate {
    val updateExampleApp = tasks.create("updateExampleApp", Copy::class.java) {
        from("../build/js/packages")
        into("../exampleJsApp/node_modules")
    }
    project.tasks.getByName("build").finalizedBy(updateExampleApp)
    updateExampleApp.dependsOn(project.tasks.getByName("build"))
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
            }
        }
    }
}

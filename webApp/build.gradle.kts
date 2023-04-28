plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val copyJsResources = tasks.create("copyJsResourcesWorkaround", Copy::class.java) {
    from(project(":shared").file("src/commonMain/resources"))
    into("build/processedResources/js/main")
}

afterEvaluate {
    project.tasks.getByName("jsProcessResources").finalizedBy(copyJsResources)
    copyJsResources.dependsOn(project.tasks.getByName("jsDevelopmentLibraryCompileSync"))
    copyJsResources.dependsOn(project.tasks.getByName("jsProductionLibraryCompileSync"))

//    project.tasks.getByName("jsNodeRun").dependsOn(project.tasks.getByName("jsDevelopmentLibraryCompileSync"))
    project.tasks.getByName("jsNodeRun").dependsOn(project.tasks.getByName("jsProductionLibraryCompileSync"))
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
        val jsMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}

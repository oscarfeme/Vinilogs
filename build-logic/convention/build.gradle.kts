plugins {
    `kotlin-dsl`
}

group = "app.vinilogs.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.composeCompiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
}

// Registers each convention plugin under a `vinilogs.*` id so module
// build.gradle.kts files can `apply(plugin = "vinilogs.android.feature")`
// (or the type-safe `alias(libs.plugins.vinilogs.android.feature)`) instead
// of repeating raw plugin + config wiring in every module.
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "vinilogs.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "vinilogs.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "vinilogs.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "vinilogs.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "vinilogs.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "vinilogs.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "vinilogs.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}

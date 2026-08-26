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
    // No compileOnly(libs.ksp.gradlePlugin): AndroidHiltConventionPlugin only applies KSP by
    // string id (pluginManager.apply("com.google.devtools.ksp")), never references its Gradle
    // plugin classes, so it doesn't need the jar on build-logic's own classpath. The version
    // resolved at consuming-module apply time comes from the root build.gradle.kts's
    // `alias(libs.plugins.ksp) apply false`. This matters because build-logic compiles with
    // kotlin-dsl's Gradle-embedded Kotlin compiler (capped well below the app's own Kotlin
    // pin — see ADR-7), which can't read newer KSP releases' binary metadata; keeping the jar
    // off this classpath sidesteps that instead of chasing a Gradle wrapper bump.
    //
    // No compileOnly(libs.hilt.gradlePlugin) either, and for the identical reason:
    // AndroidHiltConventionPlugin only applies Hilt by string id
    // (pluginManager.apply("com.google.dagger.hilt.android")), never its Gradle plugin
    // classes. hilt-android-gradle-plugin's jar (and its transitive newer kotlin-stdlib) hit
    // the exact same "incompatible version of Kotlin" wall build-logic's own compiler can't
    // read — see ADR-7's third pass.
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

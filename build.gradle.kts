// Top-level build file. Real plugin application happens in each module's
// build.gradle.kts via the convention plugins in build-logic/. Declaring the
// plugin versions here (apply false) lets every module resolve them without
// re-stating a version.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// T-05 (CI): applied to every module uniformly rather than via a build-logic
// convention plugin, since ktlint/detekt need no per-module customisation
// today. Both run with their built-in default rule sets — no project
// detekt.yml yet. Revisit as a convention plugin if per-module config is
// ever needed.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
    }
}

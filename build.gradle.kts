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
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.google.services) apply false
}

// T-05 (CI): applied to every module uniformly rather than via a build-logic
// convention plugin, since ktlint/detekt need no per-module customisation
// today. Overrides on top of the default rule sets live in config/detekt/detekt.yml
// and .editorconfig — added once the first real CI run found the defaults are
// mostly noise for a Compose UI codebase (MagicNumber on every Color(0xFF..)
// literal, FunctionNaming on every @Composable, ktlint's multiline-expression-
// wrapping fighting the project's Modifier-chain style). See those files' comments.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    }
}

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
    alias(libs.plugins.google.services) apply false
}

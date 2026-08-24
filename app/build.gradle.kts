plugins {
    alias(libs.plugins.vinilogs.android.application)
    alias(libs.plugins.vinilogs.android.application.compose)
    alias(libs.plugins.vinilogs.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:collection"))
    implementation(project(":feature:discovery"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    // Bottom-bar icons (Home/Search/Person, all in the core icon set).
    implementation(libs.androidx.compose.material.icons.core)
    // Backs Navigation Compose's type-safe routes, defined across the three
    // top-level graphs this module wires together.
    implementation(libs.kotlinx.serialization.json)
}

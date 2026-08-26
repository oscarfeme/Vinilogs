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

    // Type-safe libs.<dotted> library accessors don't resolve in the root build (Unresolved
    // reference) — findLibrary() is the same catalog, string-keyed, unaffected.
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("androidx-navigation-compose").get())
    // Bottom-bar icons (Home/Search/Person, all in the core icon set).
    implementation(libs.findLibrary("androidx-compose-material-icons-core").get())
    // Backs Navigation Compose's type-safe routes, defined across the three
    // top-level graphs this module wires together.
    implementation(libs.findLibrary("kotlinx-serialization-json").get())
}

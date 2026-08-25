plugins {
    alias(libs.plugins.vinilogs.android.application)
    alias(libs.plugins.vinilogs.android.application.compose)
    alias(libs.plugins.vinilogs.android.hilt)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:collection"))
    implementation(project(":feature:discovery"))

    // Type-safe libs.androidx.activity.compose / libs.androidx.navigation.compose accessors
    // don't resolve here (Unresolved reference: androidx) — likely the version catalog's
    // androidx-compose-ui* aliases being simultaneously a leaf and a namespace prefix breaks
    // accessor generation for the whole androidx tree. findLibrary() is the same catalog,
    // looked up by string instead of the generated accessor, and isn't affected.
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("androidx-navigation-compose").get())
}

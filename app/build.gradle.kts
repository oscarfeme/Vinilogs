plugins {
    alias(libs.plugins.vinilogs.android.application)
    alias(libs.plugins.vinilogs.android.application.compose)
    alias(libs.plugins.vinilogs.android.hilt)
}

// google-services parses app/google-services.json (per-developer, gitignored — see
// firebase/README.md) and fails the build outright if the file is missing. Applying it only
// when the file is present keeps a fresh checkout buildable before anyone has downloaded a
// real config, matching T-01's "written, not compiled" constraint.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
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
}

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
    // Type-safe libs.plugins.<dotted> accessors don't resolve in the root build here either
    // (same class of bug as the library accessors below) — findPlugin() is the same catalog,
    // string-keyed, unaffected. Matches the pattern build-logic/AndroidCompose.kt already uses.
    val googleServicesPluginId =
        libs.findPlugin("google-services")
            .get()
            .get()
            .pluginId
    apply(plugin = googleServicesPluginId)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:collection"))
    implementation(project(":feature:discovery"))

    // Type-safe libs.<dotted> library accessors don't resolve in the root build (Unresolved
    // reference) — see the fix commit on this branch. findLibrary() is the same catalog,
    // string-keyed, unaffected.
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("androidx-navigation-compose").get())
}

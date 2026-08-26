plugins {
    alias(libs.plugins.vinilogs.android.application)
    alias(libs.plugins.vinilogs.android.application.compose)
    alias(libs.plugins.vinilogs.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// google-services parses app/google-services.json (per-developer, gitignored — see
// firebase/README.md) and fails the build outright if the file is missing. Applying it only
// when the file is present keeps a fresh checkout buildable before anyone has downloaded a
// real config, matching T-01's "written, not compiled" constraint.
if (file("google-services.json").exists()) {
    // Type-safe libs.plugins.<dotted> accessors don't resolve in the root build here either
    // (same class of bug as the library accessors below) — findPlugin() is the same catalog,
    // string-keyed, unaffected. Matches the pattern build-logic/AndroidCompose.kt already uses.
    // One call per line deliberately -- ktlint's chain-wrapping rule kept disagreeing with
    // every multi-call-per-line arrangement tried here.
    val googleServicesPluginOptional = libs.findPlugin("google-services")
    val googleServicesPluginProvider = googleServicesPluginOptional.get()
    val googleServicesPlugin = googleServicesPluginProvider.get()
    apply(plugin = googleServicesPlugin.pluginId)
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

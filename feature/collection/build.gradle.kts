plugins {
    alias(libs.plugins.vinilogs.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vinilogs.feature.collection"
}

dependencies {
    // Backs this module's type-safe navigation routes. Type-safe libs.<dotted> library
    // accessors don't resolve in the root build (Unresolved reference) — findLibrary()
    // is the same catalog, string-keyed, unaffected.
    implementation(libs.findLibrary("kotlinx-serialization-json").get())

    // core:testing's Compose UI test helpers (createVinilogsComposeRule/setVinilogsContent) are
    // only wired as testImplementation by AndroidFeatureConventionPlugin -- the androidTest
    // source set (where actual Compose UI instrumented tests live) needs its own dependency.
    androidTestImplementation(project(":core:testing"))
}

// Shelf, record detail, add/edit record, stats (T-15–T-18, T-26, T-27).
// Navigation routes and stub screens for all of the above: T-03.

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

    // material-icons-core (already pulled in by core:designsystem transitively) only ships
    // ~24 "frequently used" glyphs -- no grid/list/sort/filter/share/undo. The extended pack
    // is the same androidx.compose.material Icons.Filled/Icons.AutoMirrored.Filled namespace,
    // just more of it, and its version is resolved via the already-applied Compose BOM, so
    // this doesn't add a manual version pin. Added here (T-15) rather than in core:designsystem
    // because that module is outside this track's boundary (CLAUDE.md rule 2) -- flagged in
    // the T-15 PR as a candidate to hoist up once another track needs the same icons.
    implementation(libs.findLibrary("androidx-compose-material-icons-extended").get())

    // core:testing's Compose UI test helpers (createVinilogsComposeRule/setVinilogsContent) are
    // only wired as testImplementation by AndroidFeatureConventionPlugin -- the androidTest
    // source set (where actual Compose UI instrumented tests live) needs its own dependency.
    androidTestImplementation(project(":core:testing"))
}

// Shelf, record detail, add/edit record, stats (T-15–T-18, T-26, T-27).
// Navigation routes and stub screens for all of the above: T-03.

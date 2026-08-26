plugins {
    alias(libs.plugins.vinilogs.android.library.compose)
}

android {
    namespace = "app.vinilogs.core.designsystem"
}

dependencies {
    // Type-safe libs.<dotted> accessors don't resolve for these two aliases (Unresolved
    // reference) — same version-catalog accessor-generation issue as app/build.gradle.kts
    // worked around in T-05. findLibrary() is the same catalog, string-keyed, unaffected.
    // VinylCard loads cover art via the locked image-loading decision.
    implementation(libs.findLibrary("coil-compose").get())
    // Back-arrow icon for VinilogsTopBar.
    implementation(libs.findLibrary("androidx-compose-material-icons-core").get())
}

// Theme, colour/typography/spacing tokens and shared components (VinylCard,
// CoverPlaceholder, EmptyState, ErrorState, LoadingState, VinilogsTopBar).
// No dependency on core:model — the design system doesn't know the domain.

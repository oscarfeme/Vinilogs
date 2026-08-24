plugins {
    alias(libs.plugins.vinilogs.android.library.compose)
}

android {
    namespace = "app.vinilogs.core.designsystem"
}

dependencies {
    // VinylCard loads cover art via the locked image-loading decision.
    implementation(libs.coil.compose)
    // Back-arrow icon for VinilogsTopBar.
    implementation(libs.androidx.compose.material.icons.core)
}

// Theme, colour/typography/spacing tokens and shared components (VinylCard,
// CoverPlaceholder, EmptyState, ErrorState, LoadingState, VinilogsTopBar).
// No dependency on core:model — the design system doesn't know the domain.

plugins {
    alias(libs.plugins.vinilogs.android.library.compose)
}

android {
    namespace = "app.vinilogs.core.designsystem"
}

// Theme, colour/typography/spacing tokens and shared components (VinylCard,
// CoverPlaceholder, EmptyState, ErrorState, LoadingState, VinilogsTopBar).
// No dependency on core:model — the design system doesn't know the domain.
// Populated in T-02.

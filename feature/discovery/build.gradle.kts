plugins {
    alias(libs.plugins.vinilogs.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vinilogs.feature.discovery"
}

dependencies {
    // Backs this module's type-safe navigation routes.
    implementation(libs.kotlinx.serialization.json)
}

// User search, public collections (T-23–T-25).
// Navigation routes and stub screens for all of the above: T-03.

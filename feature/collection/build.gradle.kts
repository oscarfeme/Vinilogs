plugins {
    alias(libs.plugins.vinilogs.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vinilogs.feature.collection"
}

dependencies {
    // Backs this module's type-safe navigation routes.
    implementation(libs.kotlinx.serialization.json)
}

// Shelf, record detail, add/edit record, stats (T-15–T-18, T-26, T-27).
// Navigation routes and stub screens for all of the above: T-03.

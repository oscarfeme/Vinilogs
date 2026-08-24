plugins {
    alias(libs.plugins.vinilogs.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vinilogs.feature.auth"
}

dependencies {
    // Backs this module's type-safe navigation routes.
    implementation(libs.kotlinx.serialization.json)
}

// Sign up, sign in, forgot password, profile (T-09, T-19).
// Navigation routes and stub screens for all of the above: T-03.

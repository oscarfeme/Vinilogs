plugins {
    alias(libs.plugins.vinilogs.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vinilogs.feature.auth"
}

dependencies {
    // Backs this module's type-safe navigation routes. Type-safe libs.<dotted> library
    // accessors don't resolve in the root build (Unresolved reference) — findLibrary()
    // is the same catalog, string-keyed, unaffected.
    implementation(libs.findLibrary("kotlinx-serialization-json").get())
}

// Sign up, sign in, forgot password, profile (T-09, T-19).
// Navigation routes and stub screens for all of the above: T-03.

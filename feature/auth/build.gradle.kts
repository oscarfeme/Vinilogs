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

    // T-09: mapping AuthRepository's Result<T> failures to specific, helpful copy
    // (wrong password, email already in use, ...) needs Firebase Auth's exception
    // types (AuthErrorMapper.kt). This does not call any Firebase API directly —
    // compiling against it needs no google-services.json / live project.
    implementation(platform(libs.findLibrary("firebase-bom").get()))
    implementation(libs.findLibrary("firebase-auth").get())

    // T-09: the Compose UI tests for the new screens (SignInScreenTest, ...) run
    // instrumented, so they need core:testing's Compose test helpers on the
    // androidTest classpath, not just testImplementation (which
    // vinilogs.android.feature already wires up for the JVM ViewModel tests).
    androidTestImplementation(project(":core:testing"))
}

// Sign up, sign in, forgot password, profile (T-09, T-19).
// Navigation routes and stub screens for all of the above: T-03.

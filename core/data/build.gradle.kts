plugins {
    alias(libs.plugins.vinilogs.android.library)
    alias(libs.plugins.vinilogs.android.hilt)
}

android {
    namespace = "app.vinilogs.core.data"
}

dependencies {
    // api, not implementation: the repository interfaces added in T-07
    // (AuthRepository/CollectionRepository/UserRepository) expose core:model types
    // (User, Record, etc.) in their own public signatures, so consumers of core:data need
    // those types visible transitively.
    api(project(":core:model"))

    // Firebase (T-04) — Auth, Firestore, Storage per 00-README.md's locked backend decision.
    // BOM manages the individual SDK versions; repository implementations land in T-08/T-11/T-13.
    // Type-safe libs.<dotted> library accessors don't resolve in the root build (Unresolved
    // reference) — findLibrary() is the same catalog, string-keyed, unaffected.
    implementation(platform(libs.findLibrary("firebase-bom").get()))
    implementation(libs.findLibrary("firebase-auth").get())
    implementation(libs.findLibrary("firebase-firestore").get())
    implementation(libs.findLibrary("firebase-storage").get())
    implementation(libs.findLibrary("kotlinx-coroutines-play-services").get())
}

// Room and Retrofit (Discogs) data sources are added by the tasks that implement each source
// (T-10, T-11, T-12) rather than pinned here, since their exact config isn't specified in the
// locked docs.

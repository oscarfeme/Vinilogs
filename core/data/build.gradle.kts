plugins {
    alias(libs.plugins.vinilogs.android.library)
    alias(libs.plugins.vinilogs.android.hilt)
}

android {
    namespace = "app.vinilogs.core.data"
}

dependencies {
    implementation(project(":core:model"))
}

// Repository implementations (AuthRepository, CollectionRepository,
// UserRepository per 02-ARCHITECTURE.md §4) and their Room/Firestore/Discogs
// data sources. Those libraries (Room, Firebase, Retrofit) are added by the
// tasks that implement each source (T-04, T-08, T-10, T-11, T-12) rather than
// pinned here, since their exact config isn't specified in the locked docs.

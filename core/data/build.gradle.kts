plugins {
    alias(libs.plugins.vinilogs.android.library)
    alias(libs.plugins.vinilogs.android.hilt)
}

android {
    namespace = "app.vinilogs.core.data"
}

dependencies {
    implementation(project(":core:model"))

    // Firebase (T-04) — Auth, Firestore, Storage per 00-README.md's locked backend decision.
    // BOM manages the individual SDK versions; repository implementations land in T-08/T-11/T-13.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.kotlinx.coroutines.play.services)
}

// Room and Retrofit (Discogs) data sources are added by the tasks that implement each source
// (T-10, T-11, T-12) rather than pinned here, since their exact config isn't specified in the
// locked docs.

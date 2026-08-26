plugins {
    alias(libs.plugins.vinilogs.android.library)
    alias(libs.plugins.vinilogs.android.hilt)
}

android {
    namespace = "app.vinilogs.core.data"

    // Robolectric (RecordDaoTest, T-10) needs the merged manifest/resources on its classpath.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
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

    // Room (T-10) — local persistence, source of truth for the user's own collection (ADR-2).
    // room-compiler is a KSP annotation processor, not a regular dependency; the `vinilogs.
    // android.hilt` convention plugin (applied above) already applies the KSP Gradle plugin.
    implementation(libs.findLibrary("room-runtime").get())
    implementation(libs.findLibrary("room-ktx").get())
    ksp(libs.findLibrary("room-compiler").get())

    // Robolectric (T-10, this module only) -- gives RecordDaoTest a real Context + SQLite
    // engine to build an in-memory Room database against, on the plain JVM (no device/
    // emulator). See the version catalog comment for why this is needed instead of Room's
    // context-free BundledSQLiteDriver path. Runs as a JUnit4 test via the Vintage engine,
    // which build-logic's shared TestingConfig.kt already turns on via useJUnitPlatform().
    testImplementation(libs.findLibrary("robolectric").get())
    testImplementation(libs.findLibrary("androidx-test-core").get())
    testImplementation(libs.findLibrary("junit4").get())
    testImplementation(libs.findLibrary("junit-vintage-engine").get())
}

// Retrofit (Discogs) data source is added by the task that implements it (T-12) rather than
// pinned here, since its exact config isn't specified in the locked docs.

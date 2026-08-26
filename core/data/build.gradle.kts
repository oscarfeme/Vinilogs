import java.util.Properties

plugins {
    alias(libs.plugins.vinilogs.android.library)
    alias(libs.plugins.vinilogs.android.hilt)
    // Needed for the @Serializable Discogs DTOs (T-12) -- the runtime dependency alone isn't
    // enough, kotlinx.serialization also needs its compiler plugin applied to this module.
    alias(libs.plugins.kotlin.serialization)
}

// API keys via local.properties -> BuildConfig, never hardcoded (00-README.md rule 5). Read
// here rather than relying on Gradle's own local.properties auto-loading (that only covers
// sdk.dir) -- missing key falls back to an empty string so a fresh checkout without one still
// builds; DiscogsCatalogClient treats a blank key as a graceful, typed failure at call time,
// not a crash.
val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }
val discogsApiKey: String = localProperties.getProperty("discogs.apiKey", "")
if (discogsApiKey.isBlank()) {
    logger.warn(
        "core:data: local.properties has no 'discogs.apiKey' entry -- Discogs catalogue " +
            "search will return DiscogsFailure.MissingApiKey at runtime until one is added.",
    )
}

android {
    namespace = "app.vinilogs.core.data"

    // Library modules don't enable buildConfig by default (only `app` conventionally does) --
    // needed here so DiscogsCatalogClient can read the API key without hardcoding it (T-12).
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "DISCOGS_API_KEY", "\"$discogsApiKey\"")
    }

    // Repository unit tests mock the Firebase SDK directly (no Robolectric, no live Firebase
    // project -- see T-08's PR notes). Building small SDK value objects on that path (e.g.
    // UserProfileChangeRequest.Builder) touches Android stub methods (android.text.TextUtils)
    // that the plain android.jar throws on by default outside Robolectric. Returning defaults
    // instead of throwing is the standard escape hatch for that, per
    // https://developer.android.com/r/studio-ui/build/not-mocked.
    testOptions {
        unitTests.isReturnDefaultValues = true
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

    // Discogs client (T-12) — Retrofit + OkHttp + kotlinx.serialization converter (this
    // project's locked JSON library, not Moshi/Gson).
    implementation(libs.findLibrary("kotlinx-serialization-json").get())
    implementation(libs.findLibrary("retrofit-core").get())
    implementation(libs.findLibrary("retrofit-kotlinx-serialization-converter").get())
    implementation(libs.findLibrary("okhttp").get())
    testImplementation(libs.findLibrary("okhttp-mockwebserver").get())
}

// Room data source is added by the task that implements it (T-10) rather than pinned here,
// since its exact config isn't specified in the locked docs.

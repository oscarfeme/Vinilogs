plugins {
    // .compose, not plain vinilogs.android.library — VinilogsComposeTest.kt has @Composable
    // code and needs the Compose compiler plugin.
    alias(libs.plugins.vinilogs.android.library.compose)
}

android {
    namespace = "app.vinilogs.core.testing"
}

dependencies {
    // api, not implementation: consumers reference Record/AuthRepository/etc. directly when
    // typing test doubles and assertions (e.g. `viewModel(authRepository = FakeAuthRepository())`).
    api(project(":core:model"))
    api(project(":core:data"))
    api(project(":core:designsystem"))

    // Exposed as `api`, not `testImplementation`: this module's whole purpose
    // is to be consumed as test infrastructure by other modules' test source
    // sets (fake repositories, coroutine test rule, Compose test helpers — T-07).
    // Type-safe libs.<dotted> library accessors don't resolve in the root build (Unresolved
    // reference) — findLibrary() is the same catalog, string-keyed, unaffected.
    api(libs.findLibrary("junit-jupiter-api").get())
    api(libs.findLibrary("kotlinx-coroutines-test").get())
    api(libs.findLibrary("turbine").get())
    api(libs.findLibrary("mockk").get())

    // Compose test helpers (VinilogsComposeTest) wrap androidx.compose.ui.test's JUnit4-based
    // createComposeRule — Compose UI instrumented tests use JUnit4 project-wide (T-01's
    // androidx-compose-ui-test-junit4 catalog entry), separate from the JUnit5 unit test stack
    // above.
    api(libs.findLibrary("androidx-compose-ui-test-junit4").get())
    api(libs.findLibrary("androidx-compose-ui-test-manifest").get())
}

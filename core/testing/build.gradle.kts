plugins {
    alias(libs.plugins.vinilogs.android.library)
}

android {
    namespace = "app.vinilogs.core.testing"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))

    // Exposed as `api`, not `testImplementation`: this module's whole purpose
    // is to be consumed as test infrastructure by other modules' test source
    // sets (fake repositories, coroutine test rule, Compose test helpers — T-07).
    // Type-safe libs.<dotted> library accessors don't resolve in the root build (Unresolved
    // reference) — see the base branch's fix commit. findLibrary() is the same catalog,
    // string-keyed, unaffected.
    api(libs.findLibrary("junit-jupiter-api").get())
    api(libs.findLibrary("kotlinx-coroutines-test").get())
    api(libs.findLibrary("turbine").get())
    api(libs.findLibrary("mockk").get())
}

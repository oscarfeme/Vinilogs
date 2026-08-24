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
    api(libs.junit.jupiter.api)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.mockk)
}

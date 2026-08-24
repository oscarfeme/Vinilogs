plugins {
    alias(libs.plugins.vinilogs.android.application)
    alias(libs.plugins.vinilogs.android.application.compose)
    alias(libs.plugins.vinilogs.android.hilt)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:collection"))
    implementation(project(":feature:discovery"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
}

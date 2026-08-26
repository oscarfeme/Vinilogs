import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** `vinilogs.android.library` — base config shared by every `core:*`/`feature:*` module. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")

            extensions.configure<LibraryExtension> {
                // Each module sets its own namespace in its build.gradle.kts —
                // AGP requires a unique one per module.
                // AGP 9 removes targetSdk from the library variant's defaultConfig
                // entirely; it now lives on testOptions/lint instead.
                testOptions {
                    targetSdk = 35
                }
                lint {
                    targetSdk = 35
                }

                configureKotlinAndroid(this)
            }

            configureTesting()

            dependencies {
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
            }
        }
    }
}

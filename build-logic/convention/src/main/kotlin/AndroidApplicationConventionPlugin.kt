import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `vinilogs.android.application` — the `app` module's base Android config. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")

            extensions.configure<ApplicationExtension> {
                namespace = "app.vinilogs"

                defaultConfig {
                    applicationId = "app.vinilogs"
                    targetSdk = 35
                    versionCode = 1
                    versionName = "0.1.0"
                }

                configureKotlinAndroid(this)
            }

            configureTesting()
        }
    }
}

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `vinilogs.android.application.compose` — enables Compose on the `app` module. */
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("vinilogs.android.application")
            extensions.configure<ApplicationExtension> {
                configureAndroidCompose(this)
            }
        }
    }
}

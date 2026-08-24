import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `vinilogs.android.library.compose` — enables Compose on a library module. */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("vinilogs.android.library")
            extensions.configure<LibraryExtension> {
                configureAndroidCompose(this)
            }
        }
    }
}

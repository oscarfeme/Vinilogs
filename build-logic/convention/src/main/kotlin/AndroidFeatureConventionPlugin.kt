import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * `vinilogs.android.feature` — for `feature:*` modules. Per 02-ARCHITECTURE.md
 * §1, the UI layer (feature modules) depends on domain (`core:model`) and on
 * repository interfaces (`core:data`), and per §6 never declares raw design
 * tokens, hence `core:designsystem`. Navigation Compose and Hilt are locked
 * decisions (00-README.md) that every feature screen is built on.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("vinilogs.android.library")
            pluginManager.apply("vinilogs.android.library.compose")
            pluginManager.apply("vinilogs.android.hilt")

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:data"))
                add("implementation", project(":core:designsystem"))

                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
                add("implementation", libs.findLibrary("hilt-navigation-compose").get())

                add("testImplementation", project(":core:testing"))
            }
        }
    }
}

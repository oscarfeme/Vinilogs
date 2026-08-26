import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Wires the project-wide locked test stack (00-README.md: "Testing: JUnit5 +
 * Turbine + MockK") into every module's test source set, so individual
 * feature/data tasks don't each have to remember to add it.
 */
internal fun Project.configureTesting() {
    tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
        useJUnitPlatform()
    }

    dependencies {
        add("testImplementation", libs.findLibrary("junit-jupiter-api").get())
        add("testImplementation", libs.findLibrary("junit-jupiter-params").get())
        add("testRuntimeOnly", libs.findLibrary("junit-jupiter-engine").get())
        // Gradle 9 stopped auto-resolving the JUnit Platform launcher onto the test runtime
        // classpath (it used to be pulled in transitively) -- without this, useJUnitPlatform()
        // fails at test-execution time with "Failed to load JUnit Platform".
        add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
        add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        add("testImplementation", libs.findLibrary("turbine").get())
        add("testImplementation", libs.findLibrary("mockk").get())
    }
}

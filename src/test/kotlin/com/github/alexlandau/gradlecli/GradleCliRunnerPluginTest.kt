package com.github.alexlandau.gradlecli

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'com.github.alexlandau.gradlecli.greeting' plugin.
 */
class GradleCliRunnerPluginTest {
    @Test fun `plugin registers tasks`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.alexlandau.gradlecli.greeting")

        // Verify the result
        assertNotNull(project.tasks.findByName("cliWrapper"))
        assertNotNull(project.tasks.findByName("checkCliWrapper"))
    }
}

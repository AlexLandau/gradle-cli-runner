package com.github.alexlandau.gradlecli

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GradleCliRunnerPluginFunctionalTest {
    @Test fun `can run task`() {
        val projectDir = setUpMonoProjectTest()

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("cliWrapper")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertTrue(result.task(":cliWrapper") != null)
        assertTrue(File(projectDir, "cliw").exists())
    }

    @Test fun `it nags if the plugin is applied but cliw is missing`() {
        val projectDir = setUpMonoProjectTest()

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("build")
        runner.withProjectDir(projectDir)
        val result = runner.buildAndFail()

        val checkResult = result.task(":checkCliWrapper")
        assertNotNull(checkResult)
        assertEquals(TaskOutcome.FAILED, checkResult.outcome)
        assertTrue(result.output.contains("Please run the :cliWrapper task"))
    }
}

private fun setUpMonoProjectTest(): File {
    // Setup the test build
    val projectDir = File("build/functionalTest")
    if (projectDir.exists()) {
        projectDir.deleteRecursively()
    }
    projectDir.mkdirs()
    projectDir.resolve("settings.gradle").writeText("")
    projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('com.github.alexlandau.gradlecli.greeting')
            }
        """.trimIndent())
    return projectDir
}

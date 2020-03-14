package com.github.alexlandau.gradlecli

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// TODO: Eventually reduce some of the redundancy in these tests
class GradleCliRunnerPluginFunctionalTest {
    @Test fun `can generate the cli wrapper`() {
        val projectDir = setUpMonoProjectTest()

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("cliWrapper")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertTrue(result.task(":cliWrapper") != null)
        assertTrue(File(projectDir, "cliw").exists())
        assertTrue(File(projectDir, "cliw.bat").exists())
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

    @Test fun `Preparing a Java CLI compiles its classpath`() {
        val projectDir = setUpMonoProjectTest()
        projectDir.resolve("build.gradle").appendText("""
            apply plugin: 'java'
            
            clis {
                javaCli("helloWorld", project.sourceSets.main.runtimeClasspath, "com.example.HelloWorldCli")
            }
        """.trimIndent())

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("prepareCli_helloWorld")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertNotNull(result.task(":classes"))
    }

    @Test fun `Preparing a Java CLI records an invocation`() {
        val projectDir = setUpMonoProjectTest()
        projectDir.resolve("build.gradle").appendText("""
            apply plugin: 'java'
            
            clis {
                javaCli("helloWorld", project.sourceSets.main.runtimeClasspath, "com.example.HelloWorldCli")
            }
        """.trimIndent())

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("prepareCli_helloWorld")
        runner.withProjectDir(projectDir)
        runner.build()

        assertTrue(projectDir.resolve("build/cliRunner/invocations/helloWorld").exists())
    }

    @Test fun `Running the HelloWorldCli via cliw works`() {
        val projectDir = setUpMonoProjectTest()
        projectDir.resolve("build.gradle").appendText("""
            apply plugin: 'java'
            
            clis {
                javaCli("helloWorld", project.sourceSets.main.runtimeClasspath, "com.example.HelloWorldCli")
            }
        """.trimIndent())

        // Make the CLI wrapper to run
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments(":wrapper", ":cliWrapper")
        runner.withProjectDir(projectDir)
        runner.build()

        // TODO: Use a real implementation of this
        val cliwCommand = if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            // TODO: Improve on this
            "${projectDir.absolutePath}\\cliw.bat"
        } else {
            "./cliw"
        }
        val cliwProcess = ProcessBuilder(cliwCommand, "helloWorld")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
        cliwProcess.waitFor(1, TimeUnit.MINUTES)
        val outputLines = cliwProcess.inputStream.bufferedReader().readLines()
        try {
            assertEquals(0, cliwProcess.exitValue())
            assertTrue(outputLines.contains("Hello, world!"))
        } catch (t: Throwable) {
            println("The CLI output was:")
            println(outputLines.joinToString("\n"))
            throw RuntimeException(t)
        }
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
    // Note: We take this approach (vs. a plugins block) because we aren't always running Gradle
    // via the GradleRunner that uses our intended classpath. If we instead run gradlew via cliw,
    // this is needed to get the plugin on our classpath.
    projectDir.resolve("build.gradle").writeText("""
        buildscript {
            dependencies {
                classpath files('../libs/gradle-cli-runner.jar')
            }
        }
        apply plugin: com.github.alexlandau.gradlecli.GradleCliRunnerPlugin
        
    """.trimIndent())
    projectDir.resolve("src/main/java/com/example").mkdirs()
    projectDir.resolve("src/main/java/com/example/HelloWorldCli.java").writeText("""
        package com.example;
        
        public class HelloWorldCli {
            public static void main(String[] args) {
                System.out.println("Hello, world!");
            }
        }
    """.trimIndent())
    return projectDir
}

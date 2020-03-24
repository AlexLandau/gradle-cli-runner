package com.github.alexlandau.gradlecli

import org.gradle.testkit.runner.BuildResult
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class ProjectSetup(val dir: File) {
    fun runTasks(vararg tasks: String): BuildResult {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments(tasks.toList())
        runner.withProjectDir(dir)
        return runner.build()
    }

    fun runTasksAndFail(vararg tasks: String): BuildResult {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments(tasks.toList())
        runner.withProjectDir(dir)
        return runner.buildAndFail()
    }
}

// TODO: Eventually reduce some of the redundancy in these tests
class GradleCliRunnerPluginFunctionalTest {
    @Test fun `can generate the cli wrapper`() {
        val project = setUpMonoProjectTest()

        val result = project.runTasks("cliWrapper")

        assertTrue(result.task(":cliWrapper") != null)
        assertTrue(File(project.dir, "cliw").exists())
        assertTrue(File(project.dir, "cliw.bat").exists())
    }


    @Test fun `it nags if the plugin is applied but cliw is missing`() {
        val project = setUpMonoProjectTest()

        val result = project.runTasksAndFail("build")

        val checkResult = result.task(":checkCliWrapper")
        assertNotNull(checkResult)
        assertEquals(TaskOutcome.FAILED, checkResult.outcome)
        assertTrue(result.output.contains("Please run the :cliWrapper task"))
    }

    @Test fun `Preparing a Java CLI compiles its classpath`() {
        val project = setUpMonoProjectTest()
        project.dir.resolve("build.gradle").appendText("""
            apply plugin: 'java'
            
            clis.register("helloWorld", com.github.alexlandau.gradlecli.JavaCli) {
                className = "com.example.HelloWorldCli"
                classpath = project.sourceSets.main.runtimeClasspath
            }
        """.trimIndent())

        val result = project.runTasks("prepareCli_helloWorld")

        assertNotNull(result.task(":classes"))
    }

    @Test fun `Preparing a Java CLI records an invocation`() {
        val project = setUpMonoProjectTest()
        project.dir.resolve("build.gradle").appendText("""
            apply plugin: 'java'
            
            clis.register("helloWorld", com.github.alexlandau.gradlecli.JavaCli) {
                className = "com.example.HelloWorldCli"
                classpath = project.sourceSets.main.runtimeClasspath
            }
        """.trimIndent())

        project.runTasks("prepareCli_helloWorld")

        assertTrue(project.dir.resolve("build/cliRunner/invocations/helloWorld").exists())
    }

    @Test fun `Running the HelloWorldCli via cliw works`() {
        val project = setUpMonoProjectTest()
        project.dir.resolve("build.gradle").appendText("""
            apply plugin: 'java'
            
            clis.register("helloWorld", com.github.alexlandau.gradlecli.JavaCli) {
                className = "com.example.HelloWorldCli"
                classpath = project.sourceSets.main.runtimeClasspath
            }
        """.trimIndent())

        // Make the CLI wrapper to run
        project.runTasks(":wrapper", ":cliWrapper")

        // TODO: Use a real implementation of this
        val cliwCommand = if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            // TODO: Improve on this
            "${project.dir.absolutePath}\\cliw.bat"
        } else {
            "./cliw"
        }
        val cliwProcess = ProcessBuilder(cliwCommand, "helloWorld")
                .directory(project.dir)
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

private fun setUpMonoProjectTest(): ProjectSetup {
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
    return ProjectSetup(projectDir)
}

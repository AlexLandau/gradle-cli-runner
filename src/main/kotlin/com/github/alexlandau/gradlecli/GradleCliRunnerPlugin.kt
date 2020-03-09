package com.github.alexlandau.gradlecli

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.BasePlugin

// TODO: Handle the case of no arguments, printing usage, etc.
val CLI_WRAPPER_CONTENTS = """
#!/bin/sh

set -e

CLI_NAME=$1

./gradlew prepareCli_${"$"}{CLI_NAME} --no-daemon
./build/cliRunner/invocations/${"$"}{CLI_NAME}
""".trimStart()

class GradleCliRunnerPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.rootProject.tasks.register("cliWrapper") {
            it.doLast {
                project.rootProject.file("cliw").writeText(CLI_WRAPPER_CONTENTS)
            }
        }

        val checkCliWrapperTask = project.rootProject.tasks.register("checkCliWrapper") {
            it.doLast {
                val cliwFile = project.rootProject.file("cliw")
                if (!cliwFile.exists()) {
                    throw GradleException("The gradle-cli-runner plugin has been applied, but the root project does " +
                            "not contain a cliw file. Please run the :cliWrapper task and check in the cliw file.")
                } else if (cliwFile.isDirectory) {
                    throw GradleException("The gradle-cli-runner plugin has been applied. Please delete the cliw " +
                            "directory, run the :cliWrapper task, and check in the cliw file.")
                } else if (cliwFile.readText() != CLI_WRAPPER_CONTENTS) {
                    throw GradleException("The cliw wrapper is out-of-date or has modified contents. Please rerun the " +
                            ":cliWrapper task and check in the modified cliw file.")
                }
            }
        }
        // Make sure the check task exists in the root project so we can run checkCliWrapper on a ./gradlew build
        project.rootProject.pluginManager.apply(BasePlugin::class.java)
        project.rootProject.tasks.named("check").configure {
            it.dependsOn(checkCliWrapperTask)
        }

        val cliExtension = project.extensions.create("cli", CliExtension::class.java)

        project.tasks.addRule("Prepares the CLI <ID> for being run") { taskName ->
            if (taskName.startsWith("prepareCli_")) {
                val cliName = taskName.drop("prepareCli_".length)
                // TODO: Validate that cliName doesn't include certain characters (like /, \, ;, :)
                project.task(taskName) {
                    val theCli = cliExtension.clis[cliName]
                    if (theCli == null) {
                        throw GradleException("Unknown CLI name $cliName")
                    }
                    for (depTaskName in theCli.taskDependencies) {
                        it.dependsOn(depTaskName)
                    }
                    it.doLast {
                        // Write the invocation to the appropriate file
                        val invocation = theCli.invocation
                        val invocationFile = project.file("build/cliRunner/invocations/${cliName}")
                        invocationFile.parentFile.mkdirs()
                        invocationFile.writeText(invocation)
                    }
                }
            }
        }
    }
}

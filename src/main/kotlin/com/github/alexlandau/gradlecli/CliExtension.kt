package com.github.alexlandau.gradlecli

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskDependency

data class CliProperties(val invocation: String, val taskDependencies: List<TaskDependency>)

// TODO: Make this extensible
// TODO: Separate interface type from implementation type?
open class CliExtension {
    internal val clis = HashMap<String, CliProperties>()
    // TODO: FileCollection might not be the broadest applicable input type
    fun javaCli(cliName: String, classpath: FileCollection, className: String) {
        // TODO: Do something with this

        // TODO: Wrong
        System.lineSeparator()
        val writtenClasspath = classpath.map { it.absolutePath }.joinToString(":")
        val invocation = "java.exe $className -cp $writtenClasspath"

        val taskDependencies = listOf(classpath.buildDependencies)
        clis[cliName] = CliProperties(invocation, taskDependencies)
    }
}

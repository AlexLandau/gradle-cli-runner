package com.github.alexlandau.gradlecli

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskDependency
import java.io.File

// TODO: Invocation should probably be determined at task runtime
data class CliProperties(val invocation: String, val taskDependencies: List<TaskDependency>)

// TODO: Make this extensible
// TODO: Separate interface type from implementation type?
open class CliExtension {
    internal val clis = HashMap<String, CliProperties>()
    // TODO: FileCollection might not be the broadest applicable input type (pass to project.files?)
    fun javaCli(cliName: String, classpath: FileCollection, className: String) {
        // TODO: Should delay "evaluating" the classpath
        val writtenClasspath = classpath.map { it.absolutePath }.joinToString(File.pathSeparator)

        val invocation = "java -cp $writtenClasspath $className"

        val taskDependencies = listOf(classpath.buildDependencies)
        clis[cliName] = CliProperties(invocation, taskDependencies)
    }
}

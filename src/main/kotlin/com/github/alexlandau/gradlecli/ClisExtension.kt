package com.github.alexlandau.gradlecli

import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskDependency
import java.io.File

// TODO: Separate file for this?
interface Cli {
    fun getInvocation(): String
    fun getTaskDependencies(): List<TaskDependency>
}
// TODO: FileCollection might not be the broadest applicable input type (pass to project.files?)
class JavaCli: Cli {
    lateinit var className: String
    lateinit var classpath: FileCollection

    // Note: We have two constructors (zero-argument and two-argument) to support two usages:
    // One using Actions to configure the Cli, and one with direct instantiation.
    constructor()
    constructor(className: String, classpath: FileCollection) {
        this.className = className
        this.classpath = classpath
    }

    override fun getInvocation(): String {
        // TODO: Should make sure we're delaying "evaluating" the classpath
        val writtenClasspath = classpath.map { it.absolutePath }.joinToString(File.pathSeparator)
        return "java -cp $writtenClasspath ${className}"
    }

    override fun getTaskDependencies(): List<TaskDependency> {
        return listOf(classpath.buildDependencies)
    }
}

// TODO: Separate interface type from implementation type?
open class ClisExtension {
    internal val clis = HashMap<String, Cli>()

    // Intent here is to imitate the currently-recommended tasks.register interface for adding tasks
    // But... probably also have an alternative where you just pass in the built Cli
    // TODO: Set up restrictions on T and Action<T> appropriately
    fun <T: Cli> register(cliName: String, cliClazz: Class<T>, action: Action<T>) {
        val cliInstance: T = cliClazz.getConstructor().newInstance() as T
        action.execute(cliInstance)
        clis[cliName] = cliInstance
    }

    // TODO: Deal with multiple registrations of one name
    // TODO: Deal with invalid names
    fun <T> register(cliName: String, cli: Cli) {
        clis[cliName] = cli
    }
}

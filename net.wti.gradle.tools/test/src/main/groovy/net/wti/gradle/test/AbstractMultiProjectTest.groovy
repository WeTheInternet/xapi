package net.wti.gradle.test


import net.wti.gradle.test.api.TestBuild
import net.wti.gradle.test.api.TestFileTools
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.nio.file.Files

import static org.gradle.api.logging.LogLevel.*

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/26/18 @ 2:04 AM.
 */
abstract class AbstractMultiProjectTest<S extends AbstractMultiProjectTest<S>> extends Specification implements TestBuild, TestFileTools {


    LogLevel LOG_LEVEL = QUIET
    Boolean XAPI_DEBUG = true// Boolean.getBoolean("xapi.debug")

    abstract S selfSpec()

    private File rootDir
    private String rootProjectName
    private boolean initializedSettings

    File getRootDir() {
        this.@rootDir ?: (this.@rootDir = newTmpDir())
    }

    File newTmpDir() {
        Files.createTempDirectory(selfSpec().class.simpleName).toFile()
    }

    BuildResult runSucceed(
            LogLevel logLevel = LOG_LEVEL,
            File projectDir = getRootDir(),
            Boolean debug = XAPI_DEBUG,
            String ... tasksOrFlags
    ) {
        flush()
        List<String> args = [toFlag(logLevel), '-Dxapi.home=' + getRootDir(), '--full-stacktrace', *tasksOrFlags]

        GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withDebug(debug)
                .withArguments(args)
                .with({
                    // Forward build output to std out when -Dxapi.debug=true is set.
                    XAPI_DEBUG && it.forwardStdOutput(new PrintWriter(System.out))
                    XAPI_DEBUG && it.forwardStdError(new PrintWriter(System.err))
                    return it
                })
        .build()
    }

    BuildResult runFail(
            LogLevel logLevel = LOG_LEVEL,
            Boolean debug = XAPI_DEBUG,
            File projectDir = getRootDir(),
            String ... task
    ) {
        flush()
        List<String> args = [*task, '-Dxapi.home=' + getRootDir(), '--stacktrace', toFlag(logLevel)]
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withDebug(debug)
                .withArguments(args)
                .with({
                    // always forward stdErr when we expect to fail
                    it.forwardStdError(new PrintWriter(System.err))
                    // Forward build output to std out when -Dxapi.debug=true is set.
                    XAPI_DEBUG && it.forwardStdOutput(new PrintWriter(System.out))
                    return it
                })
                .buildAndFail()
    }

    BuildResult runHelp(
            LogLevel logLevel = LOG_LEVEL,
            String task
    ) {
        runSucceed logLevel, 'help', '--task', task
    }

    BuildResult runTasks(
            LogLevel logLevel = LOG_LEVEL
    ) {
        runSucceed logLevel, 'tasks', '--all'
    }

    @Override
    Boolean hasWorkRemaining() {
        return TestBuild.super.hasWorkRemaining() || !initializedSettings
    }

    @Override
    void doWork() {
        if (!initializedSettings) {
            initializedSettings = true
            initSettings(settingsFile)
            settingsFile << """
// from ${getClass().simpleName} ($name)
rootProject.name='${getRootProjectName()}'
"""
        }
        TestBuild.super.doWork()
    }

    String toFlag(LogLevel logLevel) {
        switch (logLevel) {
            case DEBUG:
                return '-d'
            case INFO:
                return '-i'
            case WARN:
                return '-w'
            case QUIET:
                return '-q'
            default:
                throw new UnsupportedOperationException("$logLevel not supported")
        }
    }

    String getRootProjectName() {
        return rootProjectName ?: (setRootProjectName(defaultRootProjectName))
    }

    String getDefaultRootProjectName() {
        return rootDir.name
    }

    String setRootProjectName(String name) {
        if (rootProjectName) {
            if (rootProjectName != name) {
                throw new IllegalStateException("Root project name already set to $rootProjectName; cannot set it to $name")
            }
        } else {
            rootProjectName = name
        }
        return rootProjectName
    }
}

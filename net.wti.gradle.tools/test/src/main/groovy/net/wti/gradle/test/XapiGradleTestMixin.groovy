package net.wti.gradle.test


import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import java.nio.file.Files
import java.util.concurrent.Callable

import static org.gradle.api.logging.LogLevel.*
import static org.gradle.util.GUtil.uncheckedCall

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/26/18 @ 2:04 AM.
 */
trait XapiGradleTestMixin <S extends Specification & XapiGradleTestMixin<S>> extends TestProjectDir {

    LogLevel LOG_LEVEL = QUIET
    Boolean DEBUG = true// Boolean.getBoolean("xapi.debug")

    abstract S selfSpec()

    private File settingsFile
    private File rootDir
    private String rootProjectName

    File getRootDir() {
        this.@rootDir ?: (this.@rootDir = newTmpDir())
    }

    File newTmpDir() {
        Files.createTempDirectory(selfSpec().class.simpleName).toFile()
    }

    File getSettingsFile() {
        this.@settingsFile ?: (this.@settingsFile = file('settings.gradle'))
    }


    BuildResult runSucceed(
            LogLevel logLevel = LOG_LEVEL,
            File projectDir = getRootDir(),
            boolean debug = DEBUG,
            String ... task
    ) {
        flush()
        List<String> args = [*task, '--stacktrace', toFlag(logLevel)]
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withDebug(debug)
                .withArguments(args)
                .with({
                    // Forward build output to std out when -Dxapi.debug=true is set.
                    DEBUG && it.forwardStdOutput(new PrintWriter(System.out))
                    DEBUG && it.forwardStdError(new PrintWriter(System.err))
                    return it
                })
        .build()
    }

    BuildResult runFail(
            LogLevel logLevel = LOG_LEVEL,
            boolean debug = DEBUG,
            File projectDir = getRootDir(),
            String ... task
    ) {
        flush()
        List<String> args = [*task, '--stacktrace', toFlag(logLevel)]
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withDebug(debug)
                .withArguments(args)
                .with({
                    // always forward stdErr when we expect to fail
                    it.forwardStdError(new PrintWriter(System.err))
                    // Forward build output to std out when -Dxapi.debug=true is set.
                    DEBUG && it.forwardStdOutput(new PrintWriter(System.out))
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

    /**
     * Flushes any pending source generation.
     */
    void flush() {
        if (!getSettingsFile().exists() || !settingsFile.text) {
            settingsFile << "rootProject.name='${getRootProjectName()}'\n"
        }
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

    String coerceString(Object value) {
        if (value == null) {
            return null
        } else if (value instanceof CharSequence
                || value instanceof File
                || value instanceof Number
                || value instanceof Boolean) {
            return value.toString()
        } else if (value instanceof Callable) {
            final Callable callableNotation = (Callable) value
            final Object called = uncheckedCall(callableNotation)
            return coerceString(called)
        }
        throw new IllegalArgumentException("Cannot coerce ${value.class}: $value")
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

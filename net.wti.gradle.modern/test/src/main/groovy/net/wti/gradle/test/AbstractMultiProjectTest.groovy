package net.wti.gradle.test

import net.wti.gradle.api.MinimalProjectView
import net.wti.gradle.internal.ProjectViewInternal
import net.wti.gradle.test.api.TestBuild
import net.wti.gradle.test.api.TestBuildDir
import net.wti.gradle.test.api.TestFileTools
import org.gradle.StartParameter
import org.gradle.api.Action
import org.gradle.api.Describable
import org.gradle.api.artifacts.component.BuildIdentifier
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.GradleInternal
import org.gradle.api.internal.SettingsInternal
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.reflect.ObjectInstantiationException
import org.gradle.internal.build.BuildState
import org.gradle.internal.extensibility.DefaultConvention
import org.gradle.internal.instantiation.InstanceGenerator
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.Path
import spock.lang.Specification

import java.nio.file.Files
import java.util.function.Function

import static org.gradle.api.logging.LogLevel.*

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/26/18 @ 2:04 AM.
 */
abstract class AbstractMultiProjectTest<S extends AbstractMultiProjectTest<S>> extends Specification implements TestBuild, TestFileTools, TestBuildDir, ProjectViewInternal {

    LogLevel LOG_LEVEL = QUIET
    Boolean XAPI_DEBUG = true// Boolean.getBoolean("xapi.debug")

    abstract S selfSpec()

    private File rootDir
    private String rootProjectName
    private String version = "1.0"
    private String group = "test"
    private boolean initializedSettings
    Settings settings

    DefaultConvention convention
    Gradle gradle

    private Logger logger = Logging.getLogger(getClass())

    File getRootDir() {
        this.@rootDir ?: (this.@rootDir = newTmpDir())
    }

    File newTmpDir() {
        Files.createTempDirectory(selfSpec().class.simpleName).toFile()
    }

    void setup() {
        convention = new DefaultConvention(new InstanceGenerator() {
            @Override
            <T> T newInstanceWithDisplayName(final Class<? extends T> type, final Describable displayName, final Object... parameters) throws ObjectInstantiationException {
                throw new UnsupportedOperationException("Cannot create $type")
            }

            @Override
            <T> T newInstance(final Class<? extends T> aClass, final Object... objects) throws ObjectInstantiationException {
                throw new UnsupportedOperationException("Cannot create $aClass")
            }
        })
        // initialize our mocks with some state...
        Map<String, ProjectDescriptor> mockProjects = new LinkedHashMap<>();
        Map<String, String> props = new LinkedHashMap<>()
        Function<? super String, ? extends ProjectDescriptor> fakeDescriptor
        fakeDescriptor = { String K ->
            File projectDir = ':' == K ? rootDir : new File(rootDir, K.substring(1).replace(':' as char, File.separatorChar))
            mockProjects.computeIfAbsent(K, {
                Mock(ProjectDescriptor) {
                    _ * getPath() >> K
                    _ * getProjectDir() >> projectDir
                    _ * getRootProject() >> { fakeDescriptor.apply(':') }
                }
            })
        }
        settings = Mock(SettingsInternal) {
            _ * getExtensions() >> convention
            _ * getGradle() >> Mock(GradleInternal) {
                _ * getSettings() >> settings
                _ * getServices() >> Mock(ServiceRegistry) {
                    _ * get(_) >> { args ->
                        return Mock(args[0])
                    }
                }
                _ * getOwner() >> Mock(BuildState) {
                    _ * getCurrentPrefixForProjectsInChildBuilds() >> Path.path(rootDir.name)
                    _ * getBuildIdentifier() >> Mock(BuildIdentifier) {
                        _ * getBuildPath() >> getBuildName()
                    }
                }
            }
            _ * getStartParameter() >> Mock(StartParameter) {
                _ * getProjectProperties() >> props
            }
            _ * findProject(_) >> { args ->
                Object key = args[0]
                if (key instanceof File) {
                    throw new UnsupportedOperationException("Only string keys are allowed for settings.findProject during tests.")
                }
                final String k = "${ key.startsWith(':') ? '' : ':'}$key"
                fakeDescriptor.apply(k)
            }
            _ * getRootProject() >> fakeDescriptor.apply(':')
        }
        gradle = settings.gradle
    }

    BuildResult runSucceed(
            LogLevel logLevel = LOG_LEVEL,
            File projectDir = getRootDir(),
            Boolean debug = XAPI_DEBUG,
            String ... tasksOrFlags
    ) {
        flush()
        List<String> args = [toFlag(logLevel), '-Dxapi.home=' + getRootDir(), '--full-stacktrace', *tasksOrFlags]

        logger.quiet("Invoke gradle file://{} args: {}", projectDir, args.join(" "))
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
        logger.quiet("Invoke gradle (expect failure) file://{} args: {}", projectDir, args.join(" "))
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
        boolean firstTime = !initializedSettings
        if (firstTime) {
            initializedSettings = true
            initSettings(settingsFile)
            settingsFile << """
// from ${getClass().simpleName} ${name ? "($name)" : ''}
rootProject.name='${getRootProjectName()}'
"""
            propertiesFile << """
xapiGroupId=$group
xapiVersion=$version
"""
        }
        TestBuild.super.doWork()
        if (firstTime) {
            buildFile << """
allprojects { group = "$group" }
"""
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

    String getRootProjectName() {
        return rootProjectName ?: (setRootProjectName(defaultRootProjectName))
    }

    String getDefaultRootProjectName() {
        return rootDir.name
    }

    @Override
    MinimalProjectView getRootProject() {
        return this
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

    @Override
    File getProjectDir() {
        return rootDir
    }

    @Override
    Object findProperty(String s) {
        return System.getProperty(s, System.getenv(s.toUpperCase().replace('.', '_')));
    }

    @Override
    ExtensionContainer getExtensions() {
        return convention
    }

    @Override
    Instantiator getInstantiator() {
        return DirectInstantiator.INSTANCE
    }

    @Override
    CollectionCallbackActionDecorator getDecorator() {
        return CollectionCallbackActionDecorator.NOOP;
    }

    @Override
    MinimalProjectView findView(String s) {
        if (s != ':') {
            println "Ignoring request for view $s and just returning $this"
        }
        return this
    }

    @Override
    void whenReady(Action<? super MinimalProjectView> action) {
        action.execute(this)
    }

    @Override
    String getGroup() {
        return this.@group ?: rootProjectName
    }

    void setGroup(String group) {
        this.@group = group
    }

    @Override
    String getVersion() {
        return this.@version
    }

    void setVersion(String version) {
        this.@version = version
    }

    @Override
    Logger getLogger() {
        return this.@logger
    }

    @Override
    void whenSettingsReady(final Action<Settings> callback) {
        callback.execute(settings)
    }

    @Override
    String getBuildName() {
        return rootDir.name
    }
}

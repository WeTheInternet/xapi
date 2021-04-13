package net.wti.gradle.system.service;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.impl.DefaultBuildGraph;
import net.wti.gradle.internal.system.InternalProjectCache;
import net.wti.gradle.system.tools.GradleMessages;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.wrapper.Wrapper;
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType;
import org.gradle.util.Path;
import xapi.gradle.fu.LazyString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.lang.System.identityHashCode;

/**
 * A system-wide set of gradle tools,
 * useful for controlling globally-relevant build configuration.
 *
 * The default implementation matches xapi's internal default behavior,
 * while the full xapi plugin enhances this service with additional functionality.
 *
 * Setting up GradleService in your buildscripts
 * (not necessary if you apply any xapi plugins; this is for "just give me the tools" usages)
 *
 * Step 1: Get jars on your buildscript classpath.
 *
 * Option 1A: in buildSrc/build.gradle ->
 * <pre>
 *     plugins { id 'java-library' }
 *     repositories.maven {
 *         name = 'xapiLocal'
 *         url = new URI("file://$xapiHome/repo")
 *     }
 *     dependencies {
 *         api "net.wti.gradle.tools:xapi-gradle-tools:${xapiVersion}"
 *     }
 * </pre>
 *
 * Note: $xapiHome and $xapiVersion can either be supplied by you, in checked in gradle.properties,
 * or if you know where xapi will be checked out relative to your project, you can do something like:
 * apply from: "$rootDir.parent/xapi/gradle/xapi-env.gradle"
 * This would add extension values like xapiHome and xapiVersion,
 * (plus other general purpose "need this at runtime" things that you should review before using).
 *
 *
 * // Gradle Option 2: manually in each build.gradle file:
 * buildscript {
 *     repositories.maven {
 *         name = 'xapiLocal'
 *         url = new URI("file://$xapiHome/repo")
 *     }
 *     dependencies {
 *         classpath "net.wti.gradle.tools:xapi-gradle-tools:${xapiVersion}"
 *     }
 * }
 *
 * Step 2: Apply the gradle service itself:
 * import net.wti.gradle.system.spi.GradleServiceFinder
 * GradleServiceFinder.getService(project)
 *
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/15/18 @ 4:37 AM.
 */
public interface GradleService {

    String EXT_NAME = "_xapiGradle";
    /**
     * Suggested usage:
     * <pre>
     * // Java (plugin developers):
     * GradleServiceFinder.getService(project);
     * rootProject.getTasks().named("gradleZip", Zip.class, zip-> {
     *     zip.from(myInitScripts(), copy-> copy.into("init.d"))
     * })
     * // Gradle (script users):
     * // First, follow the "setting up GradleService" details in {@link GradleService}
     *
     * </pre>
     *
     *
     */
    String CUSTOM_GRADLE_ZIP_TASK = "gradleZip";

    String GRADLE_VERSION = "5.1-x-24";

    static Path buildId(Gradle gradle) {
        return ((GradleInternal)gradle).findIdentityPath();
    }

    Project getProject();

    default ProjectView getView() {
        return ProjectView.fromProject(getProject());
    }

    default Class<? extends BuildGraph> typeBuildGraph() {
        return DefaultBuildGraph.class;
    }

    default boolean isCompositeRoot() {
        GradleInternal self = (GradleInternal) getProject().getGradle();
        return self.getRoot() == self;
    }

    default boolean hasComposite() {
        return !getProject().getGradle().getIncludedBuilds().isEmpty();
    }

    // The method below are some logical tools for performing operations on ExtensionAware objects;
    // The interface-scoped methods are based on the project this service wraps,
    // and the static-scoped methods take an arbitrary ExtensionAware object.

    default void doOnce(String key, Action<? super Project> callback) {
        doOnce(getProject(), key, callback);
    }

    default <O> O buildOnce(String key, Function<Project, ? extends O> factory) {
        return buildOnce(key, (p, k)->factory.apply(p));
    }

    default <O> O buildOnce(String key, BiFunction<Project, String, ? extends O> factory) {
        return buildOnce(getProject(), key, factory);
    }

    default <O> O buildOnce(Class<? super O> publicType, String key, Function<Project, ? extends O> factory) {
        return buildOnce(publicType, key, (p, k)->factory.apply(p));
    }

    default <O> O buildOnce(Class<? super O> publicType, String key, BiFunction<Project, String, ? extends O> factory) {
        return buildOnce(publicType, getProject(), key, factory);
    }

    default String guessPrefix() {

        // look for xapi.prefix String property.  System prop wins
        String pre = System.getProperty("xapi.prefix");
        if (pre == null) {
            // next, check the project for properties.  This should be the most common way to configure.
            pre = (String) getProject().findProperty("xapi.prefix");
        }
        if (pre == null) {
            // next, check with the environment, for global defaults. (Forced overrides go in ~/.gradle/gradle.properties)
            pre = System.getenv("xapi.prefix");
        }
        if (pre == null) {
            pre = "xapi";
        }
        return pre;
    }

    static <T extends ExtensionAware> void doOnce(T ext, String key, Action<? super T> callback) {
        if (!Boolean.TRUE.equals(ext.getExtensions().findByName(key))) {
            ext.getExtensions().add(key, true);
            callback.execute(ext);
        }
    }

    static <T extends ExtensionAware, O> O buildOnce(T ext, String key, Function<? super T, ? extends O> factory) {
        return buildOnce(null, ext, key, factory);
    }

    static <T extends ExtensionAware, O> O buildOnce(Class<? super O> publicType, T ext, String key, Function<? super T, ? extends O> factory) {
        return buildOnce(publicType, ext, key, (p, k)->factory.apply(p));
    }

    static <T extends ExtensionAware, O> O buildOnce(T ext, String key, BiFunction<? super T, String, ? extends O> factory) {
        return buildOnce(null, ext, key, factory);
    }

    @SuppressWarnings("unchecked")
    static <T extends ExtensionAware, O> O buildOnce(Class<? super O> publicType, T source, String key, BiFunction<? super T, String, ? extends O> factory) {
        InternalProjectCache<T> cache = getCache(source);
        return cache.buildOnce(publicType, source, key, factory);
    }

    @SuppressWarnings("unchecked")
    static <T extends ExtensionAware> InternalProjectCache<T> getCache(T ext) {
        final ExtensionContainer extensions = ext.getExtensions();
        InternalProjectCache<T> cache;
        final String cacheSuffix = Integer.toString(identityHashCode(InternalProjectCache.class));
        String key = InternalProjectCache.EXT_NAME + cacheSuffix;
        try {
            cache = (InternalProjectCache) extensions.findByName(key);
        } catch (ClassCastException cce) {
            Object o = extensions.findByName(key);
            try {
                cache = new InternalProjectCache<>(ext, o);
            } catch (Exception ignored) {
                // give up. stomp old cache (debugging shows this happens to be buildSrc leaving a cache on root project somehow)
                cache = new InternalProjectCache<>(ext);
            }
        }
        if (cache == null || !cache.isFrom(ext)) {
            cache = new InternalProjectCache<>(ext);
            extensions.add(key, cache);
        }
        return cache;
    }

    static String extractBuildName(Gradle gradle) {
        return ((GradleInternal)gradle).getPublicBuildPath().getBuildPath().toString();
    }

    default void configureWrapper(Project project) {
        project.getLogger().trace("scheduling wrapper configuration for {}", project);
        doOnce(project.getRootProject(), "xapi.gradle.root", root->{

            final String zipSeg = "gradle-" + GRADLE_VERSION + ".zip";
            final RegularFileProperty zipDir = project.getObjects().fileProperty()
                .convention(()->{

                    String gradleRoot = System.getProperty("xapi.gradle.root");
                    if (gradleRoot == null) {
                        gradleRoot = System.getenv("ORG_GRADLE_PROJECT_xapi.gradle.root");
                    }
                    if (gradleRoot == null) {
                        gradleRoot = "gradle/gradle-X";
                    }

                    File gradleLoc = new File(gradleRoot);
                    if (!gradleLoc.isDirectory()) {
                        gradleLoc = new File(project.getRootDir().getParent(), gradleRoot);
                        if (!gradleLoc.isDirectory()) {
                            gradleLoc = new File(project.getRootDir().getParentFile().getParent(), gradleRoot);
                        }
                    }
                    project.getLogger().trace("configuring wrapper for {} using {}", root, gradleLoc);
                    // we don't check if our final location exists here; we do it later,
                    // when deciding if we should hijack Wrapper task or not
                    return gradleLoc;
                });

            final RegularFileProperty zipFile = project.getObjects().fileProperty()
                .convention(()->
                    new File(zipDir.get().getAsFile().getParentFile(), zipSeg));

            // Need to also make a task to zip the current contents of the directory,
            // put them into a dist/gradle-v.zip, then make the wrapper dependent on this task.
            // This allows you to specify a local, custom-built gradle distribution,
            // and we'll handle zipping it up for you (so default "use custom gradle" instructions work OOTB)
            final TaskContainer rootTasks = root.getTasks();
            final TaskProvider<Zip> distZip = rootTasks.register(
                CUSTOM_GRADLE_ZIP_TASK,
                Zip.class,
                zip -> {
                    // this code is only called if `wrapper` is selected to run (it's the only place resolving distZip var)
                    final File zipLoc = zipFile.get().getAsFile();
                    zipLoc.getParentFile().mkdirs();

                    zip.from(project.files(zipDir.get().getAsFile()), spec->{
                        spec.into("gradle-" + GRADLE_VERSION);
                    });
                    zip.getDestinationDirectory().set(zipLoc.getParentFile());
                    zip.getArchiveFileName().set(zipLoc.getName());
                    zip.getInputs().property("gradleVersion", GRADLE_VERSION);
                    zip.onlyIf(t->!zipLoc.isFile());
                }
            );
            root.getPluginManager().withPlugin("wrapper", plugin -> {
                rootTasks.named("wrapper", Wrapper.class, wrapper -> {
                    wrapper.whenSelected(selected->{
                        final File loc = zipDir.get().getAsFile();
                        if (loc.isDirectory()) {
                            final File zipLoc = zipFile.get().getAsFile();
                            project.getLogger().quiet("Configuring wrapper task to point to version {} @ {}", GRADLE_VERSION, zipLoc);
                            wrapper.setGradleVersion(GRADLE_VERSION);
                            wrapper.setDistributionType(DistributionType.ALL);
                            project.getLogger().info("Using local gradle distribution: {}", loc);
                            wrapper.setDistributionUrl("file://" + zipLoc.getAbsolutePath());
                            if (!zipLoc.exists()) {
                                project.getLogger().quiet("No dist zip found @ {}, creating one", zipLoc);
                                wrapper.dependsOn(distZip.get());
                            }

                        }
                    });
                });
            });
            }
        );
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    default BuildGraph createBuildGraph() {
        final ProjectView self = getView();
        final ProjectView view = self.getRootProject();
        final BuildGraph graph = view.getInstantiator()
            .newInstance(typeBuildGraph(), this, view);
        // add a traceable "this is where constructor is being used" (above) for IDE tracing support (and refactoring support!)
        assert GradleMessages.noOpForAssertion(()->new DefaultBuildGraph(this, view));
        view.getLogger().info("Initializing build graph for {}", new LazyString(self::getDebugPath));
        return graph;
    }

    @Nonnull
    default String getXapiHome() {
        String prop = findXapiHome();
        if (prop == null) {
            throw new GradleException("-Pxapi.home is not set; inherit $xapiHome/gradle/xapi-env.gradle to have it set for you");
        }
        return prop;

    }

    @Nullable
    default String findXapiHome() {
        return findXapiHome(getProject());
    }

    static String findXapiHome(Project p) {
        Object prop = System.getProperty("xapi.home");
        if (prop == null) {
            prop = p.findProperty("xapi.home");
        }
        return (String)prop;
    }
}

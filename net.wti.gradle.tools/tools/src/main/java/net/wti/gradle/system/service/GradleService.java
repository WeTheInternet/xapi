package net.wti.gradle.system.service;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.impl.DefaultBuildGraph;
import net.wti.gradle.internal.system.InternalProjectCache;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.wrapper.Wrapper;
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Function;

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
        InternalProjectCache<T> cache = (InternalProjectCache) extensions.findByName(InternalProjectCache.EXT_NAME);
        if (cache == null || !cache.isFrom(ext)) {
            cache = new InternalProjectCache<>(ext);
            extensions.add(InternalProjectCache.EXT_NAME, cache);
        }
        return cache;
    }

    default void configureWrapper(Project project) {
        project.getLogger().trace("scheduling wrapper configuration for " + project);
        doOnce(project.getRootProject(), "xapi.gradle.root", root->{

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

            final File loc = gradleLoc;
            project.getLogger().trace("configuring wrapper for {} using {}", root, loc);
            final String gradleVersion = "5.1-x-18";
            final String zipSeg = "gradle-" + gradleVersion + ".zip";
            final File zipLoc = new File(loc.getParentFile(), zipSeg);

            // Need to also make a task to zip the current contents of the directory,
            // put them into a dist/gradle-v.zip, then make the wrapper dependent on this task.
            // This allows you to specify a local, custom-built gradle distribution,
            // and we'll handle zipping it up for you (so default "use custom gradle" instructions work OOTB)
            final TaskContainer rootTasks = root.getTasks();
            final TaskProvider<Zip> distZip = rootTasks.register(
                CUSTOM_GRADLE_ZIP_TASK,
                Zip.class,
                zip -> {
                    zipLoc.getParentFile().mkdirs();
                    zip.from(project.files(loc), spec->{
                        spec.into("gradle-" + gradleVersion);
                    });
                    zip.getDestinationDirectory().set(zipLoc.getParentFile());
                    zip.getArchiveFileName().set(zipLoc.getName());
                    zip.getInputs().property("gradleVersion", gradleVersion);
                    zip.onlyIf(t->!zipLoc.isFile());
                }
            );
            rootTasks.named("wrapper", Wrapper.class, wrapper -> {
                wrapper.whenSelected(selected->{
                    if (loc.isDirectory()) {
                        project.getLogger().quiet("Configuring wrapper task to point to version {} @ {}", gradleVersion, zipLoc);
                        wrapper.setGradleVersion(gradleVersion);
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
            }
        );
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    default BuildGraph createBuildGraph() {
        final ProjectView view = getView();
        final BuildGraph graph = view.getInstantiator()
            .newInstance(typeBuildGraph(), this, view);
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
        Object prop = System.getProperty("xapi.home");
        if (prop == null) {
            prop = getProject().findProperty("xapi.home");
        }
        return (String)prop;
    }
}

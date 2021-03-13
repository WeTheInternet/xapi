package net.wti.gradle.classpath.tasks;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.api.DefaultUsageType;
import net.wti.gradle.internal.require.api.UsageType;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.Transitivity;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.exceptions.DefaultMultiCauseException;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractXapiClasspath extends DefaultTask {

    private final Property<String> projectPath;
    private final Property<String> platform;
    private final Property<String> module;
    private final Property<UsageType> usageType;
    private final Property<Boolean> resolve;
    private final Property<Boolean> strict;
    private final Property<String> exposeConfiguration;
    private final Property<Configuration> detachedResolver;

    public AbstractXapiClasspath() {
        projectPath = getObjects().property(String.class);
        platform = getObjects().property(String.class);
        module = getObjects().property(String.class);
        usageType = getObjects().property(UsageType.class);
        strict = getObjects().property(Boolean.class);
        resolve = getObjects().property(Boolean.class);
        exposeConfiguration = getObjects().property(String.class);
        detachedResolver = getObjects().property(Configuration.class);

        projectPath.convention(getProviders().provider(()->getProject().getPath()));
        platform.convention(getProviders().provider(PlatformModule.defaultPlatform));
        module.convention(getProviders().provider(PlatformModule.defaultModule));
        usageType.convention(getProviders().provider(()-> DefaultUsageType.Api));
        strict.convention(true);
        resolve.convention(true);
        exposeConfiguration.convention("");
        detachedResolver.convention(getProviders().provider(this::extractConfiguration));

        // hm... we should probably have a callback somewhere in the build graph to add task dependencies,
        // so we can depend on the actual jar task / dependencies thereof for a given module.
        // This extra task dependency should be considered optional, as it is reasonable to want to be able to
        // build a classpath in parallel to the creation / resolution of the classpath items themselves.
        // The default should still be full task dependencies, so XapiClasspath can serve as a "wait for things to be done" task.

        final boolean[] trap = {false};
        try {
            whenSelected(ignored-> {
                enforceTaskDependencies();
            });
        } catch (NoSuchMethodError ignored) {
            // user is not using xapi gradle build... they won't get automatic hookup of classpath dependencies.
            // Install a booby-trap if they also request to resolve task dependencies.
            trap[0] = true;
        }
        getBuildGraph().whenReady(ReadyState.BEFORE_CREATED + 0x40, i->{
            if (resolve.get()) {
                if (trap[0]) {
                    throw new GradleException("Requested task " + getPath() + " to resolve dependencies, but this task " +
                            "was not resolved until the execution phase, when it is too late to add more task dependencies.\n" +
                            "To resolve this issue, eagerly resolve project('" + getProject().getPath() + "').tasks.named('" + getName() + "')");
                } else {
                    enforceTaskDependencies();
                }
            }
            expose();
        });
    }

    protected void enforceTaskDependencies() {
        // finalize our properties, and unless user has disabled task dependencies, wire those up.
        resolve.finalizeValue();
        if (resolve.get()) {
            projectPath.finalizeValue();
            platform.finalizeValue();
            module.finalizeValue();
            usageType.finalizeValue();
            strict.finalizeValue();
            exposeConfiguration.finalizeValue();
            detachedResolver.finalizeValue();
        }
    }

    public void expose() {
        exposeConfiguration.finalizeValue();
        String exposed = exposeConfiguration.getOrElse("");
        if (!exposed.isEmpty()) {
            final ProjectView view = getView();


            final Configuration config = view.getConfigurations().maybeCreate(exposed);
            config.setCanBeResolved(true);
            config.setCanBeConsumed(true);
            config.getIncoming().beforeResolve(dependencies -> {
                // make sure to add our dependencies just-in-time!
            });

            // code below not currently used, as we are adding dependencies directly into the exposed configuration.
            // we'll see if this runs into issues around attributes and node selection before we delete the code below.
//            view.getProjectGraph().whenReady(ReadyState.BEFORE_READY, hostCreated-> {
//                // wait until the project producing our classpath has totally finished:
//                getProjectPath().finalizeValue();
//                getBuildGraph().getProject(projectPath.get()).whenReady(ReadyState.AFTER_CREATED, targetCreated -> {
//                    view.getDependencies().add(exposed, getOutputs().getFiles());
//                });
//            });


        }

    }

    @Inject
    public abstract ObjectFactory getObjects();

    @Inject
    public abstract ProviderFactory getProviders();

    /**
     * Check if we should expose a configuration. This is to enable easy referencing of externally-resolved classpaths;
     * you can create projects just for resolving and exposing classpaths, to increase build execution parallelization,
     * and then refer to them as normal project dependencies.  Example:
     * <p>
     * <code><pre>
     * // create a XapiClasspathTask with an exposed configuration.
     * project(':downloads-1').tasks.register('classpathSomename', XapiClasspathTask) {
     *     exposeConfiguration.set 'exposedConfiguration'
     * }
     * // consume that exposed configuration somewhere else, as a project dependency:
     * dependencies { api project(path: ':downloads-1', configuration: 'exposedConfiguration') }
     * </code>
     * </pre>
     *
     * <p>
     * @return the Property object to mutate the configured value (if we aren't frozen yet!).
     */
    @Input
    public Property<String> getExposeConfiguration() {
        return exposeConfiguration;
    }

    @Input
    public Property<String> getProjectPath() {
        return projectPath;
    }

    @Input
    public Property<String> getPlatform() {
        return platform;
    }

    @Input
    public Property<String> getModule() {
        return module;
    }

    @Input
    public Property<Boolean> getResolve() {
        return resolve;
    }

    @Input
    public Property<Boolean> getStrict() {
        return strict;
    }

    @Input
    public Property<UsageType> getUsageType() {
        return usageType;
    }

    @Internal
    protected Property<Configuration> getDetachedResolver() {
        return detachedResolver;
    }

    @TaskAction
    public void generateClasspath() {
        // extract a detached configuration
        detachedResolver.finalizeValue();
        Configuration detached = detachedResolver.get();

        LenientConfiguration lenient = detached.getResolvedConfiguration().getLenientConfiguration();

        Map<File, ResolvedArtifact> allResolved = new LinkedHashMap<>();
        for (ResolvedDependency resolved : lenient.getAllModuleDependencies()) {
            for (ResolvedArtifact artifact : resolved.getAllModuleArtifacts()) {
                allResolved.put(artifact.getFile(), artifact);
            }
        }
        List<ModuleVersionSelector> allFailed = new ArrayList<>();

        List<Throwable> errors = new ArrayList<>();
        for (UnresolvedDependency unresolved : lenient.getUnresolvedModuleDependencies()) {
            ModuleVersionSelector selector = unresolved.getSelector();
            allFailed.add(selector);

            // prepare a g:n:v string.... this should probably be static code
            String v = selector.getVersion();
            if (v == null || v.isEmpty() || "unspecified".equals(v)) {
                v = null;
            }
            String gnv = selector.getGroup() + ":" + selector.getName() + (v == null ? "" : ":" + v);

            // when strict, we throw on errors.
            if (strict.get()) {
                errors.add(new IllegalArgumentException("Dependency " + gnv + " was not resolved", unresolved.getProblem()));
            } else {
                // not strict? just put the g:n:v back into the classpath, but with escaped : character, so consumer can guess what to do with it.
                // for now, just log warning.
                getLogger().info("Skipping unresolved dependency " + gnv + " in task " + getPath() + " of " + getProject().getPath());
                // Perhaps, create a file for unresolved items, so we can just put lines of coordinates and not mess with escaping...
            }
        }
        if (!errors.isEmpty()) {
            throw new DefaultMultiCauseException(getClass().getName() + " task " + getPath()
                    +" in " + getProject().getPath() + " has strict == true, and some dependencies failed to resolve", errors);
        }

        // abstract class is done, back to you, subtype:
        consumeClasspath(allResolved, allFailed);

    }

    protected void consumeClasspath(final Map<File, ResolvedArtifact> allResolved, final List<ModuleVersionSelector> allFailed) {

    }

    // utilities
    private Configuration extractConfiguration() {
        if (getState().getFailure() != null) {
            // hm... return, or throw?
        }
        Project targetProject = getProject().project(projectPath.get());
        ProjectView view = ProjectView.fromProject(targetProject);

        String plat = platform.get();
        String mod = module.get();
        ArchiveGraph graph = view.getProjectGraph()
                .platform(plat)
                .archive(mod);

        UsageType type = usageType.get();
        // api implies "all transitive dependencies"... we may also want to use the _only variants as well...)
        // Note: we don't actually resolve this dependency directly; we'll use a Dependency object and a detach configuration,
        // so it will be resolved by the project creating the classpath file, not the project supplying the classpath information.
        Configuration configuration = type.findProducerConfig(graph, Transitivity.api);

        Map<String, String> path = new HashMap<>();
        path.put("path", view.getPath());
        path.put("configuration", configuration.getName());
        Dependency trickyDep = view.getDependencies().project(path);
        Configuration detached;
        if (exposeConfiguration.getOrElse("").isEmpty()) {
            // hmm... if we are exposed, perhaps we should reverse this relationship?
            // that is, put the dependencies into the exposed configuration instead of creating a detached one.
            detached = getProject().getConfigurations().detachedConfiguration(trickyDep);
        } else {
            // create the exposed configuration, and add dependencies directly into it.
            detached = getProject().getConfigurations().maybeCreate(exposeConfiguration.get());
            getProject().getDependencies().add(exposeConfiguration.get(), trickyDep);
        }
        detached.setCanBeConsumed(false);
        // some tasks, like XapiClasspathFileTask, can be configured to skip resolution of dependencies,
        // in which case, we don't depend on / wait for them (if we already know where output will go once built,
        // we don't need to wait until they are dare to have a file that knows where they will be).
        if (resolve.get()) {
            dependsOn(detached);
        }
        return detached;
    }

    public ProjectView getView() {
        return ProjectView.fromProject(getProject());
    }

    public BuildGraph getBuildGraph() {
        return getView().getBuildGraph();
    }
}

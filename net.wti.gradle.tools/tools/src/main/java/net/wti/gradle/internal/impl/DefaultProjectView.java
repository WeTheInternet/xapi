package net.wti.gradle.internal.impl;

import net.wti.gradle.api.GradleCrossVersionService;
import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.spi.GradleServiceFinder;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Incubating;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.component.SoftwareComponentContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.artifacts.dsl.DefaultComponentMetadataHandler;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.project.ProjectStateInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.internal.reflect.Instantiator;
import xapi.gradle.fu.LazyString;
import xapi.gradle.java.Java;

import java.io.File;
import java.util.function.Function;

/**
 * An upgraded "veneer around a {@link Project} object,
 * exposing many populate fields from Project, as well as some internal gradle / xapi tools,
 * so we can have easy, traceable access to such things,
 * without explicitly requiring an instance of Project
 * (handy for testing, room for daring / crazy developers to play around with).
 *
 * These are attached to the Project#getExtensions as "_xapiProject",
 * to warn you that you are using an internal service.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/31/18 @ 12:50 AM.
 */
@Incubating
@SuppressWarnings("UnstableApiUsage")
public class DefaultProjectView implements ProjectView {

    private static final LazyString WTI_GRADLE = new LazyString(()-> {
        try {
            org.gradle.api.internal.component.MultiCapabilitySoftwareComponent.class.getName();
            return "true";
        } catch (NoClassDefFoundError e) {
            return "false";
        }
    });
    private final String path;
    private final ObjectFactory objects;
    private final Instantiator instantiator;
    private final ProviderFactory providers;
    private final Logger logger;
    private final TaskContainer tasks;
    private final ConfigurationContainer configurations;
    private final RepositoryHandler repositories;
    private final DependencyHandler dependencies;
    private final ArtifactHandler artifacts;
    private final Function<String, Object> propFinder;
    private final ExtensionContainer extensions;
    private final CollectionCallbackActionDecorator decorator;
    private final ProjectFinder projectFinder;
    private final ProjectLayout layout;
    private final ImmutableAttributesFactory attributesFactory;
    private final Gradle gradle;
    private final SoftwareComponentContainer components;
    private final PluginContainer plugins;

    private final Runnable ensureEvaluated;
    private final Action<Action<? super Boolean>> whenReady;
    private final Function<String, File> file;
    private final Function<Object[], ConfigurableFileCollection> files;
    private final Function<Object, FileTree> zipTree;

    private final Provider<BuildGraph> buildGraph;
    private final Provider<SourceSetContainer> sourceSets;
    private final Provider<Object> group;
    private final Provider<Object> name;
    private final Provider<Object> version;
    private final Provider<PublishingExtension> publishing;
    private final Provider<GradleService> service;
    private final DefaultComponentMetadataHandler componentMetadata;
    private static volatile Throwable lastTrace;

    public DefaultProjectView(
        Project project,
        Instantiator instantiator,
        CollectionCallbackActionDecorator dec,
        ProjectFinder finder,
        ImmutableAttributesFactory attributesFactory
    ) {
        this(
            project.getGradle(),
            project.getPath(),
            project.getObjects(),
            instantiator,
            project.getExtensions(),
            project.getPlugins(),
            project.getProviders(),
            project.getLogger(),
            project.getTasks(),
            project.getConfigurations(),
            project.getRepositories(),
            project.getDependencies(),
            project.getArtifacts(),
            project.getLayout(),
            project.getComponents(),
            dec,
            finder,
            attributesFactory,
            (DefaultComponentMetadataHandler) project.getDependencies().getComponents(),

            // objects above, lambdas below *~
            project::findProperty,
            project::file,
            project::files,
            project::zipTree,

            // only immutable values should go above here.
            runOnce(()-> {
                final boolean configuring = ((ProjectStateInternal)project.getState()).isConfiguring();
                final boolean executed = project.getState().getExecuted();
                project.getLogger().info("project {} already executed? {}; isConfiguring? {}", project.getPath(), executed, configuring);
                if (!configuring && !executed) {
                    Logger log = project.getLogger();
                    log.quiet("forcibly evaluating project {}", project.getPath());
                    if (log.isEnabled(LogLevel.INFO)) {
                        log.info("Evaluation occurred via debugging stack trace:", new Exception("project evaluation trace", lastTrace));
                    }
                    ((ProjectInternal)project).evaluate();
                }
            }),
            // whenReady lambda definition: try to execute as eagerly as possible
            done-> {
                if (project.getState().getExecuted()) {
                    // Hm. Consider making this always-async using a higher level queue somewhere.
                    // Note that this avoids crossing the MutationGuard in project.afterEvaluate,
                    // so it's safe to call generically / after the mutable Project has been finalized.
                    done.execute(true);
                } else {
                    project.afterEvaluate(p -> done.execute(false));
                }
            },
            // only providers should go below here

            project.getProviders().provider(()->Java.sources(project)),
            project.getProviders().provider(()-> GradleServiceFinder.getService(project)),
            project.getProviders().provider(()-> {

                final PublishingExtension result = project.getExtensions().findByType(PublishingExtension.class);
                if (result == null) {
                    throw new GradleException("You must add a publishing plugin like maven-publish, or call Project.extensions.add('publishing', new DefaultPublishingExtension())");
                }
                return result;
            }),
            project.getProviders().provider(project::getGroup),
            project.getProviders().provider(project::getName),
            project.getProviders().provider(project::getVersion),
            project.getProviders().provider(()->BuildGraph.findBuildGraph(project))
        );
    }

    private static Runnable runOnce(Runnable r) {
        Runnable[] pntr = { r };
        return ()-> {
            Runnable task;
            synchronized (pntr) {
                task = pntr[0];
                pntr[0] = ()->{};
                task.run();
            }
        };
    }

    public DefaultProjectView(
        Gradle gradle,
        String path,
        ObjectFactory objects,
        Instantiator instantiator,
        ExtensionContainer extensions,
        PluginContainer plugins,
        ProviderFactory providers,
        Logger logger,
        TaskContainer tasks,
        ConfigurationContainer configurations,
        RepositoryHandler repositories,
        DependencyHandler dependencies,
        ArtifactHandler artifacts,
        ProjectLayout layout,
        SoftwareComponentContainer components,
        CollectionCallbackActionDecorator decorator,
        ProjectFinder projectFinder,
        ImmutableAttributesFactory attributesFactory,
        DefaultComponentMetadataHandler componentMetadata,
        // All "okay to resolve immediately" final values, above.
        Function<String, Object> propFinder,
        Function<String, File> file,
        Function<Object[], ConfigurableFileCollection> files,
        Function<Object, FileTree> zipTree,
        Runnable ensureEvaluated,

        Action<Action<? super Boolean>> whenReady,
        // All new providers should go here, at the end
        Provider<SourceSetContainer> sourceSets,
        Provider<GradleService> service,
        Provider<PublishingExtension> publishing,
        Provider<Object> group,
        Provider<Object> name,
        Provider<Object> version,
        Provider<BuildGraph> buildGraph
    ) {
        this.gradle = gradle;
        this.path = path;
        this.objects = objects;
        this.instantiator = instantiator;
        this.extensions = extensions;
        this.plugins = plugins;
        this.providers = providers;
        this.logger = logger == null ? defaultLogger() : logger;
        this.tasks = tasks;
        this.configurations = configurations;
        this.repositories = repositories;
        this.dependencies = dependencies;
        this.artifacts = artifacts;
        this.components = components;
        this.decorator = decorator;
        this.projectFinder = projectFinder;
        this.attributesFactory = attributesFactory;
        this.componentMetadata = componentMetadata;
        this.layout = layout;

        this.propFinder = propFinder;
        this.whenReady = whenReady;
        this.file = file;
        this.files = files;
        this.zipTree = zipTree;
        this.ensureEvaluated = ensureEvaluated;

        // The rest are providers.  Make them lazy.
        this.sourceSets = lazyProvider(sourceSets);
        this.buildGraph = lazyProvider(buildGraph);
        this.service = lazyProvider(service);
        this.group = lazyProvider(group, true);
        this.name = lazyProvider(name, true);
        this.version = lazyProvider(version, true).map(v-> {
            if ("unspecified".equals(v)) {
                v = findProperty("xapiVersion");
            }
            if (v == null || "".equals(v)) {
                // TODO: get a decent default version
                getLogger().warn("Version accessed but not configured in {} of build {}", getPath(), GradleService.buildId(gradle), new RuntimeException());
                return "0.0.1";
            }
            return v;
        });
        this.publishing = lazyProvider(publishing);
    }

    private Logger defaultLogger() {
        final Logger log = Logging.getLogger(DefaultProjectView.class);
        log.warn("Project View {} did not have a logger, using a generic for-class logger", getPath());
        return log;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public ObjectFactory getObjects() {
        return objects;
    }

    @Override
    public Instantiator getInstantiator() {
        return instantiator;
    }

    @Override
    public ProviderFactory getProviders() {
        return providers;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public TaskContainer getTasks() {
        return tasks;
    }

    @Override
    public ConfigurationContainer getConfigurations() {
        return configurations;
    }

    @Override
    public RepositoryHandler getRepositories() {
        return repositories;
    }

    @Override
    public DependencyHandler getDependencies() {
        return dependencies;
    }

    @Override
    public SoftwareComponentContainer getComponents() {
        return components;
    }

    @Override
    public ArtifactHandler getArtifacts() {
        return artifacts;
    }

    @Override
    public SourceSetContainer getSourceSets() {
        return sourceSets.get();
    }

    @Override
    public Object findProperty(String named) {
        return propFinder.apply(named);
    }

    @Override
    public ProjectView findProject(String named) {
        if (named.isEmpty()) {
            named = ":";
        }
        if (named.equals(getPath())) {
            return this;
        }
        final GradleCrossVersionService migration = getGradleVersionService();
        if (!DefaultProjectView.isWtiGradle()) {
            return ProjectView.fromProject(migration.findProject(projectFinder, named));
        }
        final ProjectInternal proj = migration.findProject(projectFinder, named);
        if (proj == null) {
            if (":schema".equals(named) || ":xapi-schema".equals(named)) {
                // TODO use a boolean parameter instead of ugly hardcoded "quieter failure" code here
                getLogger().debug("Unable to find project {}", named);
            } else {
                getLogger().error("Unable to find project {}", named);
            }
            return null;
        }
        return ProjectView.fromProject(proj);
    }

    @Override
    public CollectionCallbackActionDecorator getDecorator() {
        return decorator;
    }

    @Override
    public ExtensionContainer getExtensions() {
        return extensions;
    }

    @Override
    public PluginContainer getPlugins() {
        return plugins;
    }

    @Override
    public BuildGraph getBuildGraph() {
        return buildGraph.get();
    }

    @Override
    public ProjectLayout getLayout() {
        return layout;
    }

    @Override
    public Gradle getGradle() {
        return gradle;
    }

    @Override
    public ImmutableAttributesFactory getAttributesFactory() {
        return attributesFactory;
    }

    @Override
    public PublishingExtension getPublishing() {
        return publishing.get();
    }

    @Override
    public String getGroup() {
        final Object g = group.get();
        String group = String.valueOf(g);
        return group.isEmpty() ? "group.not.set.in." + getPath().replace(':', '_') : group;
    }

    @Override
    public String getName() {
        final Object n = name.get();
        return String.valueOf(n);
    }

    @Override
    public final File file(String path) {
        return file.apply(path);
    }

    @Override
    public final ConfigurableFileCollection files(Object... from) {
        return files.apply(from);
    }

    @Override
    public void ensureEvaluated() {
        ensureEvaluated.run();
    }

    @Override
    public final FileTree zipTree(Object from) {
        return zipTree.apply(from);
    }

    @Override
    public void whenReady(Action<? super MinimalProjectView> callback) {
        Throwable check = new Throwable();
        // make this semi-expensive operation contingent upon info log levels
        if (gradle.getRootProject().getLogger().isEnabled(LogLevel.INFO)) {
            check.fillInStackTrace();
        }
        whenReady.execute(immediate-> {
            Throwable was = lastTrace;
            lastTrace = check;
            try {
                callback.execute(this);
            } finally {
                if (lastTrace == check) {
                    lastTrace = was;
                }
            }
        });
    }

    @Override
    public String getVersion() {
        Object v = version.get();
        return String.valueOf(v);
    }

    @Override
    public GradleService getService() {
        return service.get();
    }

    @Override
    public Settings getSettings() {
        return ((GradleInternal)getGradle()).getSettings();
    }

    @Override
    public DefaultComponentMetadataHandler getComponentMetadata() {
        return componentMetadata;
    }


    public static boolean isWtiGradle() {
        return GradleCoerce.unwrapBoolean(WTI_GRADLE);
    }

    @Override
    public String toString() {
        return "DefaultProjectView{" +
            "path='" + path + '\'' +
            ", version=" + (version.isPresent() ? getVersion() : "<not resolved yet>") +
            '}';
    }

}

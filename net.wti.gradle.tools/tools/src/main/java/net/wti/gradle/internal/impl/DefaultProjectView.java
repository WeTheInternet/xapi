package net.wti.gradle.internal.impl;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.BuildGraph;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.component.SoftwareComponentContainer;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.internal.reflect.Instantiator;
import xapi.gradle.java.Java;

import java.util.function.Function;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/31/18 @ 12:50 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public class DefaultProjectView implements ProjectView {

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
    private final Provider<SourceSetContainer> sourceSets;
    private final Function<String, Object> propFinder;
    private final ExtensionContainer extensions;
    private final CollectionCallbackActionDecorator decorator;
    private final Provider<BuildGraph> buildGraph;
    private final ProjectFinder projectFinder;
    private final ProjectLayout layout;
    private final ImmutableAttributesFactory attributesFactory;
    private final Gradle gradle;
    private final Provider<Object> group;
    private final Provider<Object> name;
    private final Provider<Object> version;
    private final SoftwareComponentContainer components;

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
            project.getProviders(),
            project.getLogger(),
            project.getTasks(),
            project.getConfigurations(),
            project.getRepositories(),
            project.getDependencies(),
            project.getArtifacts(),
            project.getLayout(),
            project.getComponents(),
            project.getProviders().provider(()->Java.sources(project)),
            project::findProperty,
            dec,
            finder,
            attributesFactory,
            project.getProviders().provider(project::getGroup),
            project.getProviders().provider(project::getName),
            project.getProviders().provider(project::getVersion),
            project.getProviders().provider(()->BuildGraph.findBuildGraph(project))
        );
    }

    public DefaultProjectView(
        Gradle gradle,
        String path,
        ObjectFactory objects,
        Instantiator instantiator,
        ExtensionContainer extensions,
        ProviderFactory providers,
        Logger logger,
        TaskContainer tasks,
        ConfigurationContainer configurations,
        RepositoryHandler repositories,
        DependencyHandler dependencies,
        ArtifactHandler artifacts,
        ProjectLayout layout,
        SoftwareComponentContainer components,
        Provider<SourceSetContainer> sourceSets,
        Function<String, Object> propFinder,
        CollectionCallbackActionDecorator decorator,
        ProjectFinder projectFinder,
        ImmutableAttributesFactory attributesFactory,
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
        this.providers = providers;
        this.logger = logger;
        this.tasks = tasks;
        this.configurations = configurations;
        this.repositories = repositories;
        this.dependencies = dependencies;
        this.artifacts = artifacts;
        this.components = components;
        this.sourceSets = sourceSets;
        this.propFinder = propFinder;
        this.decorator = decorator;
        this.projectFinder = projectFinder;
        this.attributesFactory = attributesFactory;
        this.buildGraph = buildGraph;
        this.layout = layout;
        this.group = group;
        this.name = name;
        this.version = version;
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
        if (named.equals(getPath())) {
            return this;
        }
        final ProjectInternal proj = projectFinder.findProject(named);
        if (proj == null) {
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
    public String getGroup() {
        final Object g = group.get();
        return String.valueOf(g);
    }

    @Override
    public String getName() {
        final Object n = name.get();
        return String.valueOf(n);
    }

    @Override
    public String getVersion() {
        final Object v = version.get();
        if ("unspecified".equals(v)) {
            // TODO: get a decent default version
            getLogger().warn("Version accessed but not configured in {}", getPath(), new RuntimeException());
            return "0.0.1";
        }
        return String.valueOf(v);
    }


    @Override
    public String toString() {
        return "DefaultProjectView{" +
            "path='" + path + '\'' +
            ", version=" + getVersion() +
            '}';
    }
}

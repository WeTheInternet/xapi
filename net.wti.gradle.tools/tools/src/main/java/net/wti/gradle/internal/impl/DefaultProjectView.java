package net.wti.gradle.internal.impl;

import net.wti.gradle.internal.api.ProjectView;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.logging.Logger;
import org.gradle.api.model.ObjectFactory;
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
    private final SourceSetContainer sourceSets;
    private final Function<String, Object> propFinder;

    public DefaultProjectView(
        Instantiator instantiator, Project project) {
        this(
            project.getPath(),
            project.getObjects(),
            instantiator,
            project.getProviders(),
            project.getLogger(),
            project.getTasks(),
            project.getConfigurations(),
            project.getRepositories(),
            project.getDependencies(),
            project.getArtifacts(),
            Java.sources(project),
            project::findProperty
        );
    }

    public DefaultProjectView(
        String path,
        ObjectFactory objects,
        Instantiator instantiator,
        ProviderFactory providers,
        Logger logger,
        TaskContainer tasks,
        ConfigurationContainer configurations,
        RepositoryHandler repositories,
        DependencyHandler dependencies,
        ArtifactHandler artifacts,
        SourceSetContainer sourceSets,
        Function<String, Object> propFinder
    ) {
        this.path = path;
        this.objects = objects;
        this.instantiator = instantiator;
        this.providers = providers;
        this.logger = logger;
        this.tasks = tasks;
        this.configurations = configurations;
        this.repositories = repositories;
        this.dependencies = dependencies;
        this.artifacts = artifacts;
        this.sourceSets = sourceSets;
        this.propFinder = propFinder;
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
    public ArtifactHandler getArtifacts() {
        return artifacts;
    }

    @Override
    public SourceSetContainer getSourceSets() {
        return sourceSets;
    }

    @Override
    public Object findProperty(String named) {
        return propFinder.apply(named);
    }
}

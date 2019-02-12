package net.wti.gradle.internal.require.impl;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.system.api.RealizableNamedObjectContainer;
import org.gradle.api.Action;

import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/1/19 @ 1:03 AM.
 */
public class DefaultProjectGraph extends AbstractChildGraphNode<PlatformGraph, BuildGraph> implements ProjectGraph {

    private final BuildGraph graph;
    private final ProjectView project;

    public DefaultProjectGraph(BuildGraph graph, ProjectView project) {
        super(PlatformGraph.class, graph, project);
        this.graph = graph;
        this.project = project;
        graph.bindLifecycle(this);
    }


    @Override
    protected PlatformGraph createItem(String name) {
        return new DefaultPlatformGraph(this, name);
    }

    @Override
    public BuildGraph root() {
        return graph;
    }

    @Override
    public ProjectView project() {
        return project;
    }

    @Override
    public RealizableNamedObjectContainer<PlatformGraph> platforms() {
        return super.getItems();
    }

    @Override
    public void realizedPlatforms(Action<? super PlatformGraph> action) {
        whenRealized(action);
    }

    @Override
    public Set<PlatformGraph> realizedPlatforms() {
        return realizedItems();
    }

    @Override
    public Set<String> registeredPlatforms() {
        return registeredItems();
    }

    @Override
    public PlatformGraph main() {
        return platforms().maybeCreate("main");
    }

    @Override
    public PlatformGraph test() {
        return platforms().maybeCreate("test");
    }

    @Override
    public String getName() {
        return project.getPath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final DefaultProjectGraph that = (DefaultProjectGraph) o;

        return project.getPath().equals(that.project.getPath());
    }

    @Override
    public int hashCode() {
        return project.getPath().hashCode();
    }

    @Override
    public String toString() {
        return "DefaultProjectGraph{" +
            // later, when main() is configurable, we _may_ want to do this:
//            "main=" + main().getName() +
            "name='" + getName() + '\'' +
            "} " + super.toString();
    }

}

package net.wti.gradle.require.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.require.internal.BuildGraph.PlatformGraph;
import net.wti.gradle.require.internal.BuildGraph.ProjectGraph;
import org.gradle.api.NamedDomainObjectContainer;

import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/1/19 @ 1:03 AM.
 */
public class DefaultProjectGraph extends AbstractBuildGraphNode<PlatformGraph> implements ProjectGraph {
    private final BuildGraph graph;
    private final ProjectView project;

    public DefaultProjectGraph(BuildGraph graph, ProjectView project) {
        super(PlatformGraph.class, project.getInstantiator());
        this.graph = graph;
        this.project = project;
    }

    @Override
    protected PlatformGraph createItem(String name) {
        return null;
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
    public NamedDomainObjectContainer<PlatformGraph> platforms() {
        return super.getItems();
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
}

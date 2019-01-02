package net.wti.gradle.require.internal;

import net.wti.gradle.require.internal.BuildGraph.ArchiveGraph;
import net.wti.gradle.require.internal.BuildGraph.ArchiveRequest;
import net.wti.gradle.require.internal.BuildGraph.PlatformGraph;
import net.wti.gradle.require.internal.BuildGraph.ProjectGraph;
import net.wti.gradle.system.tools.GradleCoerce;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/1/19 @ 2:20 AM.
 */
public class DefaultPlatformGraph extends AbstractBuildGraphNode<ArchiveGraph> implements PlatformGraph {

    private final ProjectGraph project;
    private final String name;
    // Consider reworking api such that we can make this final
    private PlatformGraph parent;

    public DefaultPlatformGraph(ProjectGraph parent, String name) {
        super(ArchiveGraph.class, parent.project().getInstantiator());
        this.project = parent;
        this.name = name;
    }

    public DefaultPlatformGraph(PlatformGraph parent, String name) {
        this(parent.project(), name);
        this.parent = parent;
    }

    @Override
    protected ArchiveGraph createItem(String name) {
        return null;
    }

    @Override
    public ProjectGraph project() {
        return project;
    }

    @Override
    public PlatformGraph parent() {
        return parent;
    }

    @Override
    public ArchiveGraph archive(Object name) {
        final String key = GradleCoerce.unwrapString(name);
        return getItems().maybeCreate(key);
    }

    @Override
    public Set<ArchiveGraph> realize() {
        getItems().all(ignored->{});
        Set<ArchiveGraph> all = new LinkedHashSet<>();
        getItems().whenObjectAdded(all::add);
        getItems().whenObjectRemoved(all::remove);
        return all;
    }

    @Override
    public Set<ArchiveGraph> realizedArchives() {
        return realizedItems();
    }

    @Override
    public Set<ArchiveRequest> incoming() {
        return Collections.emptySet();
    }

    @Override
    public Set<ArchiveRequest> outgoing() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return name;
    }
}

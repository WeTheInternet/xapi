package net.wti.gradle.internal.require.impl;

import net.wti.gradle.internal.require.api.*;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.internal.SourceMeta;
import net.wti.gradle.system.api.RealizableNamedObjectContainer;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Action;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/1/19 @ 2:20 AM.
 */
public class DefaultPlatformGraph extends AbstractChildGraphNode<ArchiveGraph, ProjectGraph> implements PlatformGraph {

    private final ProjectGraph project;
    private final String name;
    // Consider reworking api such that we can make this final
    private PlatformGraph parent;

    public DefaultPlatformGraph(ProjectGraph parent, String name) {
        super(ArchiveGraph.class, parent, parent.project());
        this.project = parent;
        this.name = name;
    }

    public DefaultPlatformGraph(PlatformGraph parent, String name) {
        this(parent.project(), name);
        this.parent = parent;
    }

    @Override
    protected ArchiveGraph createItem(String name) {
        return new DefaultArchiveGraph(this, name);
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
        return getItems().maybeCreate(key == null ? "main" : key);
    }

    @Override
    public RealizableNamedObjectContainer<ArchiveGraph> archives() {
        return getItems();
    }

    @Override
    public void realizedArchives(Action<? super ArchiveGraph> action) {
        whenRealized(action);
    }

    @Override
    public Set<ArchiveGraph> realize() {
        Set<ArchiveGraph> all = new LinkedHashSet<>();
        getItems().realizeInto(all);
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
        // Consider a `:project/` prefix for this name...
        // Really not what we want for user dsl,
        // but may make sense to supply an alternate Namer for any compositing collections
        // (i.e. if there's a global all platforms collection, it can use a prefixing Namer)
        return name;
    }

    @Override
    public PlatformConfigInternal config() {
        return (PlatformConfigInternal)
            getView().getSchema().getPlatforms().getByName(getName());
    }

    @Override
    public void setParent(PlatformGraph parent) {
        this.parent = parent;
    }

    @Override
    public SourceMeta sourceFor(
        SourceSetContainer srcs, ArchiveGraph archive
    ) {
        final String srcName = archive.getSrcName();
        SourceSet src = srcs.findByName(srcName);
        SourceMeta meta = null;
        if (src == null) {
            src = srcs.create(srcName);
        } else {
            meta = (SourceMeta) src.getExtensions().findByName(SourceMeta.EXT_NAME);
        }
        if (meta == null) {
            meta = new SourceMeta(this, archive, src);
            src.getExtensions().add(SourceMeta.EXT_NAME, meta);
        }
        return meta;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final DefaultPlatformGraph that = (DefaultPlatformGraph) o;

        if (!project.equals(that.project))
            return false;
        if (!name.equals(that.name))
            return false;
        return Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        int result = project.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (parent != null ? parent.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DefaultPlatformGraph{" +
            "project=" + project.getPath() +
            ", name='" + name + '\'' +
            ", parent=" + (parent == null ? "null" : parent.getName()) +
            "} ";
    }
}

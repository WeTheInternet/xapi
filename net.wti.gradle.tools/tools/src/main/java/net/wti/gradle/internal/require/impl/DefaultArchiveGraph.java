package net.wti.gradle.internal.require.impl;

import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.ArchiveRequest;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import net.wti.gradle.internal.require.api.ModuleTasks;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.require.api.DependencyKey;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/5/19 @ 2:36 AM.
 */
public class DefaultArchiveGraph implements ArchiveGraph {
    private final PlatformGraph platform;
    private final String name;
    private final Set<ArchiveRequest> incoming;
    private final Set<ArchiveRequest> outgoing;
    private final ModuleTasks tasks;

    public DefaultArchiveGraph(PlatformGraph platform, String name) {
        this.platform = platform;
        this.name = name;
        incoming = new LinkedHashSet<>();
        outgoing = new LinkedHashSet<>();
        tasks = new ModuleTasks(this);
    }

    @Override
    public PlatformGraph platform() {
        return platform;
    }

    @Override
    public ArchiveRequest request(
        ArchiveGraph other, ArchiveRequestType type
    ) {
        throw new UnsupportedOperationException("ArchiveGraph.request() not yet implemented");
    }

    @Override
    public Map<DependencyKey, ?> id() {
        throw new UnsupportedOperationException("ArchiveGraph.id() not yet implemented");
    }

    @Override
    public File srcRoot() {
        return new File(platform.getView().getProjectDir(), "src/" + getSrcName());
    }

    @Override
    public ModuleTasks getTasks() {
        return tasks;
    }

    @Override
    public Set<ArchiveRequest> getIncoming() {
        return incoming;
    }

    @Override
    public Set<ArchiveRequest> getOutgoing() {
        return outgoing;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final DefaultArchiveGraph that = (DefaultArchiveGraph) o;

        if (!platform.equals(that.platform))
            return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = platform.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DefaultArchiveGraph{" +
            "platform=" + platform +
            ", name='" + name + '\'' +
            '}';
    }
}

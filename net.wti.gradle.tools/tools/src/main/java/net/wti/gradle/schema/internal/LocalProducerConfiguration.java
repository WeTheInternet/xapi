package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProducerConfiguration;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.require.api.PlatformModule;

public class LocalProducerConfiguration implements ProducerConfiguration {

    private final ProjectGraph graph;
    private final PlatformModule coords;

    public LocalProducerConfiguration(ProjectGraph graph, PlatformModule coords) {
        this.graph = graph;
        this.coords = coords;
    }

    @Override
    public PlatformModule getProducerCoords() {
        return coords;
    }

    @Override
    public ArchiveGraph getProducerModule() {
        PlatformGraph platform = graph.platform(coords.getPlatform());
        @SuppressWarnings("UnnecessaryLocalVariable") // nice for debugging
        ArchiveGraph module = platform.archive(coords.getModule());
        return module;
    }
}

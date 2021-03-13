package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.ProducerConfiguration;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.SchemaDependency;

public class ExternalProducerConfiguration implements ProducerConfiguration {

    private final SchemaDependency dependency;

    public ExternalProducerConfiguration(SchemaDependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public PlatformModule getProducerCoords() {
        return dependency.getCoords();
    }

    @Override
    public ArchiveGraph getProducerModule() {
        return null;
    }

    @Override
    public String getName() {
        return dependency.getGroup();
    }
}

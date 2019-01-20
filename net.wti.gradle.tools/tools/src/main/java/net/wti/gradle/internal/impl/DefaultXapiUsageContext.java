package net.wti.gradle.internal.impl;

import net.wti.gradle.internal.api.XapiUsageContext;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import org.gradle.api.artifacts.*;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Usage;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal;
import org.gradle.api.internal.artifacts.configurations.Configurations;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.internal.impldep.com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/13/19 @ 1:10 PM.
 */
@SuppressWarnings("UnstableApiUsage")
public class DefaultXapiUsageContext implements XapiUsageContext {

    private final ArchiveGraph archive;
    private final Usage usage;
    private final Configuration configuration;
    private final ImmutableAttributes attributes;
    private ImmutableSet<? extends Capability> capabilities;
    private Set<? extends ModuleDependency> dependencies;
    private Set<? extends DependencyConstraint> dependencyConstraints;
    private ImmutableSet<ExcludeRule> excludeRules;

    public DefaultXapiUsageContext(ArchiveGraph archive, String usage) {
        this.archive = archive;
        this.usage = archive.getView().getObjects().named(Usage.class, usage);
        final ImmutableAttributesFactory factory = archive.getView().getAttributesFactory();
        attributes = factory.of(Usage.USAGE_ATTRIBUTE, this.usage);
        // For java-library compatibility, we want to support JAVA_API|RUNTIME.
        // We may add more dynamic contexts later as we diverge further down the road.
        switch (usage) {

            // compile
            case Usage.JAVA_API:
                this.configuration = archive.configExportedApi();
                break;

            // runtime
            case Usage.JAVA_RUNTIME:
                this.configuration = archive.configExportedRuntime();
                break;

            default:
                throw new UnsupportedOperationException("Usage context " + usage + " not yet supported");
        }

    }

    @Override
    public Usage getUsage() {
        return usage;
    }

    @Override
    public Set<? extends PublishArtifact> getArtifacts() {
        return configuration.getOutgoing().getArtifacts();
    }

    @Override
    public Set<? extends ModuleDependency> getDependencies() {
        if (dependencies == null) {
            dependencies = configuration.getIncoming().getDependencies().withType(ModuleDependency.class);
        }
        return dependencies;
    }

    @Override
    public Set<? extends DependencyConstraint> getDependencyConstraints() {
        if (dependencyConstraints == null) {
            dependencyConstraints = configuration.getIncoming().getDependencyConstraints();
        }
        return dependencyConstraints;
    }

    @Override
    public Set<? extends Capability> getCapabilities() {
        if (capabilities == null) {
            this.capabilities = ImmutableSet.copyOf(Configurations.collectCapabilities(configuration,
                new HashSet<>(),
                new HashSet<>()));
        }
        return capabilities;
    }

    @Override
    public Set<ExcludeRule> getGlobalExcludes() {
        if (excludeRules == null) {
            this.excludeRules = ImmutableSet.copyOf(((ConfigurationInternal) configuration).getAllExcludeRules());
        }
        return excludeRules;
    }

    @Override
    public String getName() {
        return archive.getSrcName();
    }

    @Override
    public AttributeContainer getAttributes() {
        return attributes;
    }
}

package net.wti.gradle.internal.variants.impl;

import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.repositories.metadata.MavenImmutableAttributesFactory;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.internal.component.external.model.DefaultConfigurationMetadata;
import org.gradle.internal.component.external.model.ModuleComponentResolveMetadata;
import org.gradle.internal.component.external.model.VariantDerivationStrategy;
import org.gradle.internal.component.external.model.maven.DefaultMavenModuleResolveMetadata;
import org.gradle.internal.component.model.ConfigurationMetadata;
import org.gradle.internal.impldep.com.google.common.collect.ImmutableList;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/3/19 @ 1:34 AM.
 */
public class XapiVariantStrategy implements VariantDerivationStrategy {

    @Override
    public boolean derivesVariants() {
        return true;
    }

    @Override
    public ImmutableList<? extends ConfigurationMetadata> derive(ModuleComponentResolveMetadata metadata) {
        if (metadata instanceof DefaultMavenModuleResolveMetadata) {
            DefaultMavenModuleResolveMetadata md = (DefaultMavenModuleResolveMetadata) metadata;
            ImmutableAttributes attributes = md.getAttributes();
            MavenImmutableAttributesFactory attributesFactory = (MavenImmutableAttributesFactory) md.getAttributesFactory();
            DefaultConfigurationMetadata compileConfiguration = (DefaultConfigurationMetadata) md.getConfiguration("compile");
            DefaultConfigurationMetadata runtimeConfiguration = (DefaultConfigurationMetadata) md.getConfiguration("runtime");
            return ImmutableList.of(
                libraryWithUsageAttribute(compileConfiguration, attributes, attributesFactory, Usage.JAVA_API),
                libraryWithUsageAttribute(runtimeConfiguration, attributes, attributesFactory, Usage.JAVA_RUNTIME),
                platformWithUsageAttribute(compileConfiguration, attributes, attributesFactory, Usage.JAVA_API, false),
                platformWithUsageAttribute(runtimeConfiguration, attributes, attributesFactory, Usage.JAVA_RUNTIME, false),
                platformWithUsageAttribute(compileConfiguration, attributes, attributesFactory, Usage.JAVA_API, true),
                platformWithUsageAttribute(runtimeConfiguration, attributes, attributesFactory, Usage.JAVA_RUNTIME, true));
        }
        return null;
    }

    private static ConfigurationMetadata libraryWithUsageAttribute(DefaultConfigurationMetadata conf, ImmutableAttributes originAttributes, MavenImmutableAttributesFactory attributesFactory, String usage) {
        ImmutableAttributes attributes = attributesFactory.libraryWithUsage(originAttributes, usage);
        return conf.withAttributes(attributes).withoutConstraints();
    }

    private static ConfigurationMetadata platformWithUsageAttribute(DefaultConfigurationMetadata conf, ImmutableAttributes originAttributes, MavenImmutableAttributesFactory attributesFactory, String usage, boolean enforcedPlatform) {
        ImmutableAttributes attributes = attributesFactory.platformWithUsage(originAttributes, usage, enforcedPlatform);
        String prefix = enforcedPlatform ? "enforced-platform-" : "platform-";
        DefaultConfigurationMetadata metadata = conf.withAttributes(prefix + conf.getName(), attributes);
        metadata = metadata.withConstraintsOnly();
        if (enforcedPlatform) {
            metadata = metadata.withForcedDependencies();
        }
        return metadata;
    }
}


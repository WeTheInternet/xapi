package net.wti.gradle.internal.variants.impl;

import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.attributes.Usage;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.internal.artifacts.repositories.metadata.MavenImmutableAttributesFactory;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.internal.component.external.model.*;
import org.gradle.internal.component.external.model.DefaultConfigurationMetadata.Builder;
import org.gradle.internal.component.external.model.maven.DefaultMavenModuleResolveMetadata;
import org.gradle.internal.component.model.ConfigurationMetadata;
import org.gradle.internal.impldep.com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

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
            ModuleComponentIdentifier componentId = md.getId();
            List<Capability> shadowedPlatformCapability = buildShadowPlatformCapability(componentId);
            return ImmutableList.of(
                libraryWithUsageAttribute(compileConfiguration, attributes, attributesFactory, Usage.JAVA_API),
                libraryWithUsageAttribute(runtimeConfiguration, attributes, attributesFactory, Usage.JAVA_RUNTIME),
                platformWithUsageAttribute(compileConfiguration, attributes, attributesFactory, Usage.JAVA_API, false, shadowedPlatformCapability),
                platformWithUsageAttribute(runtimeConfiguration, attributes, attributesFactory, Usage.JAVA_RUNTIME, false, shadowedPlatformCapability),
                platformWithUsageAttribute(compileConfiguration, attributes, attributesFactory, Usage.JAVA_API, true, shadowedPlatformCapability),
                platformWithUsageAttribute(runtimeConfiguration, attributes, attributesFactory, Usage.JAVA_RUNTIME, true, shadowedPlatformCapability));
        }
        return null;
    }

    private List<Capability> buildShadowPlatformCapability(ModuleComponentIdentifier componentId) {
        return Collections.singletonList(
            new DefaultShadowedCapability(new ImmutableCapability(
                componentId.getGroup(),
                componentId.getModule(),
                componentId.getVersion()
            ), "-derived-platform")
        );
    }

    private static ConfigurationMetadata libraryWithUsageAttribute(DefaultConfigurationMetadata conf, ImmutableAttributes originAttributes, MavenImmutableAttributesFactory attributesFactory, String usage) {
        ImmutableAttributes attributes = attributesFactory.libraryWithUsage(originAttributes, usage);
        return conf.mutate()
            .withAttributes(attributes)
            .withoutConstraints()
            .build();
    }

    private static ConfigurationMetadata platformWithUsageAttribute(
        DefaultConfigurationMetadata conf,
        ImmutableAttributes originAttributes,
        MavenImmutableAttributesFactory attributesFactory,
        String usage,
        boolean enforcedPlatform,
        List<Capability> shadowedPlatformCapability
    ) {
        ImmutableAttributes attributes = attributesFactory.platformWithUsage(originAttributes, usage, enforcedPlatform);
        String prefix = enforcedPlatform ? "enforced-platform-" : "platform-";
        Builder builder = conf.mutate();
        builder = builder
            .withName(prefix + conf.getName())
            .withAttributes(attributes)
            .withConstraintsOnly()
            .withCapabilities(shadowedPlatformCapability)
            ;
        if (enforcedPlatform) {
            builder = builder.withForcedDependencies();
        }
        return builder.build();
    }
}

// TODO: delete this on next gradle bump, since we've made the method creating them `public static`
class DefaultShadowedCapability implements ShadowedCapability {
    private final CapabilityInternal shadowed;
    private final String appendix;

    DefaultShadowedCapability(CapabilityInternal shadowed, String appendix) {
        this.shadowed = shadowed;
        this.appendix = appendix;
    }

    @Override
    public String getAppendix() {
        return appendix;
    }

    @Override
    public CapabilityInternal getShadowedCapability() {
        return shadowed;
    }

    @Override
    public String getGroup() {
        return shadowed.getGroup();
    }

    @Override
    public String getName() {
        return shadowed.getName() + appendix;
    }

    @Override
    public String getVersion() {
        return shadowed.getVersion();
    }

    @Override
    public String getCapabilityId() {
        return shadowed.getCapabilityId() + appendix;
    }
}

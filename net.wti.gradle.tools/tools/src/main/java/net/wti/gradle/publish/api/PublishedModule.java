package net.wti.gradle.publish.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.XapiUsageContext;
import net.wti.gradle.internal.api.XapiVariant;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.component.ComponentWithCoordinates;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.component.MultiCapabilitySoftwareComponent;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Contains the publishing metadata for a given {@link ArchiveGraph}.
 *
 * Each published module corresponds to a unique group:name pair.
 * For now, the version is derived from the project, and not configurable / considered.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/26/19 @ 3:18 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public class PublishedModule implements XapiVariant, ComponentWithCoordinates, MultiCapabilitySoftwareComponent {

    private final String name;
    private final DomainObjectSet<XapiUsageContext> usages;
    private final ModuleVersionIdentifier coordinates;
    private final String group;
    private final String moduleName;
    private final ArchiveGraph module;

    public PublishedModule(
        ProjectView view,
        ArchiveGraph module,
        ModuleVersionIdentifier coordinates,
        String name
    ) {
        this.coordinates = coordinates;
        this.name = name;
        this.module = module;
        this.group = module.getGroup();
        this.moduleName = module.getModuleName();
        this.usages = new DefaultDomainObjectSet<>(XapiUsageContext.class, view.getDecorator());
    }

    @Override
    public DomainObjectSet<XapiUsageContext> getUsages() {
        return usages;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ModuleVersionIdentifier getCoordinates() {
        return coordinates;
    }

    public String getGroup() {
        return group;
    }

    public String getModuleName() {
        return moduleName;
    }

    public ArchiveGraph getModule() {
        return module;
    }

    @Nullable
    @Override
    public ModuleVersionIdentifier findCapabilityForConfiguration(
        ModuleVersionIdentifier candidate, String configurationName
    ) {
        for (XapiUsageContext usage : usages) {
            final Set<? extends Capability> capabilities = usage.getCapabilities();
            for (Capability capability : capabilities) {
//                if (
//                    capability.getName().equals(candidate.getName()) &&
//                    capability.getGroup().equals(candidate.getGroup()) &&
//                    capability.getVersion().equals(candidate.getVersion())
//                ) {
                    if (configurationName.equals(usage.getConfigurationName())) {
                        ArchiveGraph graph = usage.getModule();
                        return DefaultModuleVersionIdentifier.newId(capability.getGroup(),
                            graph.getModuleName(), capability.getVersion());
                    }
                }
//            }

        }

        return null;
    }
}

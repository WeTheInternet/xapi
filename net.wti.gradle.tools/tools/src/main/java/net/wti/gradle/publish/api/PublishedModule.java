package net.wti.gradle.publish.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.XapiUsageContext;
import net.wti.gradle.internal.api.XapiVariant;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.component.ComponentWithCoordinates;
import org.gradle.api.internal.DefaultDomainObjectSet;

/**
 * Contains the publishing metadata for a given {@link ArchiveGraph}.
 *
 * Each published module corresponds to a unique group:name pair.
 * For now, the version is derived from the project, and not configurable / considered.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/26/19 @ 3:18 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public class PublishedModule implements XapiVariant, ComponentWithCoordinates {

    private final String name;
    private final DomainObjectSet<XapiUsageContext> usages;
    private final ModuleVersionIdentifier coordinates;
    private final String group;
    private final String moduleName;

    public PublishedModule(
        ProjectView view,
        ArchiveGraph arch,
        ModuleVersionIdentifier coordinates,
        String name
    ) {
        this.coordinates = coordinates;
        this.name = name;
        this.group = arch.getGroup();
        this.moduleName = arch.getModuleName();
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
}

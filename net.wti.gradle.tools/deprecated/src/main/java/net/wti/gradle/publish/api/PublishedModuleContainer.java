package net.wti.gradle.publish.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.XapiPlatform;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.system.impl.DefaultRealizableNamedObjectContainer;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;

import javax.annotation.Nonnull;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/27/18 @ 12:04 AM.
 */
public class PublishedModuleContainer extends DefaultRealizableNamedObjectContainer<PublishedModule> {

    private final ProjectView view;
    private final XapiPlatform platform;

    public PublishedModuleContainer(ProjectView view, XapiPlatform xapiPlatform) {
        super(PublishedModule.class, view.getInstantiator(), view.getDecorator());
        this.platform = xapiPlatform;
        this.view = view;
    }

    @Override
    protected PublishedModule doCreate(@Nonnull String name) {
        final String platName = platform.getName();
        final PlatformGraph plat = view.getProjectGraph().platform(platName);
        final ArchiveGraph arch = plat.archive(name);
        final String version = view.getVersion();
        final ModuleVersionIdentifier coordinates = DefaultModuleVersionIdentifier.newId(
            arch.getGroup(), arch.getModuleName(), version);
        return new PublishedModule(view, arch, coordinates, name);
    }

}

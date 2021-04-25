package net.wti.gradle.publish.impl;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.XapiPlatform;
import net.wti.gradle.publish.api.PublishedModule;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.CompositeDomainObjectSet;

import javax.annotation.Nonnull;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/27/18 @ 12:04 AM.
 */
public class XapiPlatformContainer extends AbstractNamedDomainObjectContainer<XapiPlatform> {

    private final ProjectView view;
    private CompositeDomainObjectSet<PublishedModule> all;

    protected XapiPlatformContainer(
        ProjectView view,
        CompositeDomainObjectSet<PublishedModule> all
    ) {
        super(XapiPlatform.class, view.getInstantiator(), view.getDecorator());
        this.view = view;
        this.all = all;
    }

    @Override
    protected XapiPlatform doCreate(@Nonnull String name) {
        final XapiPlatform platform = new XapiPlatform(view, name);
        all.addCollection(platform.getModules());
        return platform;
    }

    public CompositeDomainObjectSet<PublishedModule> getAllVariants() {
        return all;
    }
}

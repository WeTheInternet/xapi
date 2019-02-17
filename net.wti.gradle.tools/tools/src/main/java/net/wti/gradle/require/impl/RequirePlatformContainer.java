package net.wti.gradle.require.impl;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.require.api.RequirePlatform;
import net.wti.gradle.require.api.XapiRequire;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.system.tools.GradleMessages;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/28/19 @ 2:51 AM.
 */
public class RequirePlatformContainer extends AbstractNamedDomainObjectContainer<RequirePlatform> {

    private final ProjectView view;
    private final XapiRequire require;

    public RequirePlatformContainer(ProjectView view, XapiRequire require) {
        super(RequirePlatform.class, view.getInstantiator(), view.getDecorator());
        this.view = view;
        this.require = require;
    }

    @Override
    protected RequirePlatform doCreate(String name) {
        final XapiSchema schema = view.getSchema();
        final PlatformConfigInternal platform = schema.findPlatform(name);
        if (platform == null) {
            throw new IllegalArgumentException("Platform " + name + " not found in schema: " + schema);
        }
        // Create a platform-scoped XapiRequire
        assert GradleMessages.noOpForAssertion(()->new RequirePlatform(view, require, name));
        return view.getInstantiator().newInstance(RequirePlatform.class, view, require, name);
    }

}

package net.wti.gradle.require.impl;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.require.api.RequirePlatform;
import net.wti.gradle.require.api.XapiRequire;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.system.service.GradleService;
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
        assert !"version".equals(name);
        schema.whenReady(ready->{
            // Push this validation later, since we want to be able to call xapiRequire _before_ the schema callbacks are invoked.
            final PlatformConfigInternal platform = schema.findPlatform(name);
            if (platform == null && !"true".equals(System.getProperty("idea.version"))) {
                throw new IllegalArgumentException(
                    (":".equals(view.getPath()) ? view.getProjectDir() : view.getPath())
                    + " -> platform " + name + " not found in schema: " + schema);
            }
        });
        assert GradleMessages.noOpForAssertion(()->new RequirePlatform(view, require, name));
        // ^ This noOp method doesn't invoke new RequirePlatform, the newInstance(), below, does;
        // this allows IDE tracing of constructor to lead you here, to the place we construct this object.
        // Create a platform-scoped XapiRequire:
        return view.getInstantiator().newInstance(RequirePlatform.class, view, require, name);
    }

}

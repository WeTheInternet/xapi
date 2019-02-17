package net.wti.gradle.require.impl;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.require.api.RequireModule;
import net.wti.gradle.require.api.RequirePlatform;
import net.wti.gradle.require.api.XapiRequire;
import net.wti.gradle.system.tools.GradleMessages;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/28/19 @ 2:51 AM.
 */
public class RequireModuleContainer extends AbstractNamedDomainObjectContainer<RequireModule> {

    private final ProjectView view;
    private final RequirePlatform platform;
    private final XapiRequire require;

    public RequireModuleContainer(
        ProjectView view,
        XapiRequire require,
        RequirePlatform platform
    ) {
        super(RequireModule.class, view.getInstantiator(), view.getDecorator());
        this.view = view;
        this.platform = platform;
        this.require = require;
    }

    @Override
    protected RequireModule doCreate(String name) {
        // Create a module-scoped XapiRequire
        assert GradleMessages.noOpForAssertion(()->new RequireModule(view, require, platform, name));
        return view.getInstantiator().newInstance(RequireModule.class, view, require, platform, name);
    }
}

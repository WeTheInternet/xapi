package net.wti.gradle.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.Requirable;
import net.wti.gradle.require.impl.RequireModuleContainer;
import net.wti.gradle.schema.internal.XapiRegistration;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectList;

/**
 * A xapiRequire DSL for a specific platform:
 * xapiRequire {
 *     // The gwt{} block here is a RequirePlatform; xapiRequire itself is a container for RequirePlatforms
 *     gwt {
 *         // applies to _all_ gwt platform archives
 *         external 'a:b:c'
 *         main {
 *             // applies only to gwt:main archive.
 *             external 'x.y.z'
 *         }
 *     }
 * }
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/4/19 @ 10:45 PM.
 */
public class RequirePlatform extends BaseRequire<RequireModule> implements Named, Requirable {

    private final String name;
    private final RequireModuleContainer modules;
    private final NamedDomainObjectList<XapiRegistration> registrations;
    private final ProjectView view;

    public RequirePlatform(
        ProjectView view,
        XapiRequire require,
        String name
    ) {
        this.name = name;
        this.modules = new RequireModuleContainer(view, require, this);
        registrations = require.getRegistrations();
        this.view = view;
    }

    @Override
    public Object getDefaultPlatform() {
        return getName();
    }

    @Override
    public ProjectView getView() {
        return view;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NamedDomainObjectList<XapiRegistration> getRegistrations() {
        return registrations;
    }

    @Override
    protected NamedDomainObjectContainer<RequireModule> container() {
        return modules;
    }
}

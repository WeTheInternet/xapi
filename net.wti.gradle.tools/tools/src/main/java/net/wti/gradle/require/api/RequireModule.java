package net.wti.gradle.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.Requirable;
import net.wti.gradle.schema.internal.XapiRegistration;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectList;

/**
 * A xapiRequire DSL for a specific module:
 * xapiRequire {
 *     // The gwt{} block here is a RequirePlatform; xapiRequire itself is a container for RequirePlatforms
 *     gwt {
 *         // applies to _all_ gwt platform archives
 *         external 'a:b:c'
 *         main {
 *             // applies only to gwt:main module.
 *             external 'x.y.z'
 *         }
 *     }
 * }
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/4/19 @ 10:45 PM.
 */
public class RequireModule implements Named, Requirable {

    private final String name;
    private final RequirePlatform platform;
    private final NamedDomainObjectList<XapiRegistration> registrations;
    private final ProjectView view;

    public RequireModule(
        ProjectView view,
        XapiRequire require,
        RequirePlatform platform,
        String name
    ) {
        this.platform = platform;
        this.name = name;
        this.registrations = require.getRegistrations();
        this.view = view;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getDefaultPlatform() {
        return platform.getName();
    }

    @Override
    public ProjectView getView() {
        return view;
    }

    @Override
    public Object getDefaultModule() {
        return getName();
    }

    @Override
    public NamedDomainObjectList<XapiRegistration> getRegistrations() {
        return registrations;
    }

    @Override
    public void internal(Object project) {
        Requirable.super.internal(project);
    }

    @Override
    public void external(Object project) {
        Requirable.super.external(project);
    }
}

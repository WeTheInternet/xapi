package net.wti.gradle.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.Requirable;
import net.wti.gradle.require.impl.RequirePlatformContainer;
import net.wti.gradle.schema.internal.XapiRegistration;
import org.gradle.api.Named.Namer;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.internal.DefaultNamedDomainObjectList;

/**
 * An extension object to enable easy "hands off" wiring of dependencies.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 10:32 PM.
 */
public class XapiRequire extends BaseRequire<RequirePlatform> implements Requirable {

    public static final String EXT_NAME = "xapiRequire";

    private final NamedDomainObjectList<XapiRegistration> registrations;
    private final NamedDomainObjectContainer<RequirePlatform> platforms;
    private final ProjectView view;

    public XapiRequire(ProjectView view) {
        this.registrations = view.getGradleVersionService().namedDomainList(
                XapiRegistration.class,
                view.getInstantiator()
        );
        this.view = view;
        platforms = new RequirePlatformContainer(view, this);
    }

    @Override
    public ProjectView getView() {
        return view;
    }

    @Override
    public NamedDomainObjectList<XapiRegistration> getRegistrations() {
        return registrations;
    }

    @Override
    protected NamedDomainObjectContainer<RequirePlatform> container() {
        return platforms;
    }

    public RequireModule main() {
        return main("main"); // make the default main platform name configurable / Property-driven
    }

    public RequireModule main(String platform) {
        return module(platform, "main");
    }

    public RequireModule platform(CharSequence platform) {
        return module(platform, "main");
    }

    public RequireModule module(CharSequence platform, CharSequence module) {
        return platforms.maybeCreate(platform.toString()).getProperty(module.toString());
    }

    public RequireModule module(CharSequence module) {
        return module("main", module);
    }
}

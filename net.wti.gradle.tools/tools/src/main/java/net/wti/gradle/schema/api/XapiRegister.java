package net.wti.gradle.schema.api;

import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.internal.XapiRegistration;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;

/**
 * An extension object to enable easy "hands off" wiring of dependencies.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 10:32 PM.
 */
@SuppressWarnings("UnstableApiUsage")
public class XapiRegister {

    public static final String EXT_NAME = "xapiRequire";

    private final ListProperty<XapiRegistration> registrations;
    private final XapiSchema schema;

    public XapiRegister(XapiSchema schema, ObjectFactory factory) {
        this.schema = schema;
        this.registrations = factory.listProperty(XapiRegistration.class);
    }

    public void project(Object project) {
        XapiRegistration reg = XapiRegistration.from(project, null, null);
        registrations.add(reg);
    }

    public void project(Object project, Object platform, Object archive) {
        XapiRegistration reg = XapiRegistration.from(project, platform, archive);
        registrations.add(reg);
    }

    public Object propertyMissing(String name) {
        // missing properties will get treated as "getPlatform" calls.
        final PlatformConfigInternal platform = schema.findPlatform(name);
        if (platform == null) {
            throw new IllegalArgumentException("Platform " + name + " not found in schema: " + schema);
        }
        return platform;
    }

    public Object methodMissing(String name, Object args) {
        final PlatformConfigInternal platform = (PlatformConfigInternal) propertyMissing(name);
        if (args instanceof Object[]) {
            platform.getMainArchive().require((Object[]) args);
        }
        return platform;
    }

    public ListProperty<XapiRegistration> getRegistrations() {
        return registrations;
    }
}

package net.wti.gradle.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.internal.XapiRegistration;
import org.gradle.api.Named.Namer;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.internal.DefaultNamedDomainObjectList;

/**
 * An extension object to enable easy "hands off" wiring of dependencies.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 10:32 PM.
 */
@SuppressWarnings("UnstableApiUsage")
public class XapiRequire {

    public static final String EXT_NAME = "xapiRequire";

    private final NamedDomainObjectList<XapiRegistration> registrations;
    private final XapiSchema schema;

    public XapiRequire(ProjectView project) {
        this.schema = project.getSchema();
        this.registrations = new DefaultNamedDomainObjectList<>(
            XapiRegistration.class,
            project.getInstantiator(),
            Namer.forType(XapiRegistration.class)
        );
    }

    /**
     * Create inter-project dependencies between all platform and archive types.
     *
     * Beware that using this will force-realize all possible configurations and sourcesets.
     * It is likely to perform better to call {@link #project(Object, Object, Object)} multiple times,
     * if you know that you don't need every possible permutation of every possible archive type.
     *
     * @param project An object to resolve into a :project:path when the {@link XapiRegistration} is resolved.
     */
    public void project(Object project) {
        XapiRegistration reg = XapiRegistration.from(project, null, null);
        registrations.add(reg);
    }

    /**
     * Create an inter-project dependency on a specific project, platform and archive.
     *
     * This will bind a set of dependencies on same-named configurations in the consumed project.
     *
     * Specifically, the myPlatMyArchive(Transitive|Implementation|Runtime) configurations.
     * In terms of gradle's java-library plugin:
     * Transitive ~= apiElements
     * Implementation ~= implementation
     * Runtime ~= runtimeElements
     *
     * When used on the root xapiRequire, we will create and bind any such sourcesets;
     * when used on a scoped xapiRequire, the target configuration is sourced from the scope,
     * with a fallback to "use same-named configurations in current project".
     *
     * @param project An object from which to derive a :project:path
     * @param platform An object from which to derive a platform name; null -> "main"|"test"
     * @param archive An object from which to derive the archive type; null -> "main"|"test"
     */
    public void project(Object project, Object platform, Object archive) {
        XapiRegistration reg = XapiRegistration.from(project, platform, archive);
        registrations.add(reg);
    }

//
//    public void external(Object path, Object ... into) {
//        if (into == null || into.length == 0) {
//            into = new Object[]{"main"};
//        }
//        Callable[] each = new Callable[into.length];
//        for (int i = 0; i < into.length; i++) {
//            final Object o = into[i];
//            each[i] = ()-> GradleCoerce.unwrapString(o);
//        }
//
//    }

    public Object propertyMissing(String name) {
        // missing properties will get treated as "getPlatform" calls.
        final PlatformConfigInternal platform = schema.findPlatform(name);
        if (platform == null) {
            throw new IllegalArgumentException("Platform " + name + " not found in schema: " + schema);
        }
        // Create a platform-scoped XapiRequire

        return platform;
    }

    public Object methodMissing(String name, Object args) {
        final PlatformConfigInternal platform = (PlatformConfigInternal) propertyMissing(name);
        if (args instanceof Object[]) {
            platform.getMainArchive().require((Object[]) args);
        }
        return platform;
    }

    public NamedDomainObjectList<XapiRegistration> getRegistrations() {
        return registrations;
    }
}

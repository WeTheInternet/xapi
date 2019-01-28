package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.internal.XapiRegistration;
import net.wti.gradle.schema.internal.XapiRegistration.RegistrationMode;
import org.gradle.api.NamedDomainObjectList;

import java.util.Map;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/28/19 @ 3:05 AM.
 */
public interface Requirable {

    /**
     * Create inter-project dependencies between all platform and archive types.
     *
     * Beware that using this will force-realize all possible configurations and sourcesets.
     * It is likely to perform better to call {@link #project(Object, Object, Object)} multiple times,
     * if you know that you don't need every possible permutation of every possible archive type.
     *
     * @param project An object to resolve into a :project:path when the {@link XapiRegistration} is resolved.
     */
    default void project(Object project) {
        XapiRegistration reg = XapiRegistration.from(project, getDefaultPlatform(), getDefaultModule(), getInto(), RegistrationMode.project);
        getRegistrations().add(reg);
    }

    default Object getDefaultModule() {
        return null;
    }

    default Object getDefaultPlatform() {
        return null;
    }

    default Object getInto() {
        return null;
    }

    default void external(Object project) {
        XapiRegistration reg = XapiRegistration.from(project, getDefaultPlatform(), getDefaultModule(), getInto(), RegistrationMode.external);
        getRegistrations().add(reg);
    }

    default void internal(Object project) {
        if (project instanceof Map) {
            final Map map = (Map) project;
            project = map.get("platform") + ":" + map.get("module");
        }
        XapiRegistration reg = XapiRegistration.from(project,
            getDefaultPlatform(), getDefaultModule(), project, RegistrationMode.internal
        );
        getRegistrations().add(reg);
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
    default void project(Object project, Object platform, Object archive) {
        XapiRegistration reg = XapiRegistration.from(project, platform, archive, getInto());
        getRegistrations().add(reg);
    }

    ProjectView getView();

    NamedDomainObjectList<XapiRegistration> getRegistrations();
}

package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.system.api.RealizableNamedObjectContainer;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectProvider;

import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/2/19 @ 4:21 AM.
 */
public interface ProjectGraph extends Named {
    BuildGraph root();

    ProjectView project();

    RealizableNamedObjectContainer<PlatformGraph> platforms();

    default PlatformGraph platform(CharSequence name) {
        return platforms().maybeCreate(name.toString());
    }

    NamedDomainObjectProvider<PlatformGraph> getOrRegister(String platform);

    void realizedPlatforms(Action<? super PlatformGraph> action);

    Set<PlatformGraph> realizedPlatforms();

    Set<String> registeredPlatforms();

    PlatformGraph main();

    PlatformGraph test();

    default boolean isSelectable(PlatformGraph platform) {
        Object prop = project().findProperty("xapi.platform");
        if (prop == null || "".equals(prop)) {
            return true;
        }
        String requested = GradleCoerce.unwrapString(prop);
        return platform.matches(requested);
    }

    default String getPath() {
        return project().getPath();
    }

    default String getGroup() {
        return project().getVersion();
    }
}

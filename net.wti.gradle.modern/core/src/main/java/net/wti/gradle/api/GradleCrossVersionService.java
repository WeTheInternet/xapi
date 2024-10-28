package net.wti.gradle.api;

import net.wti.gradle.internal.GradleCrossVersionServiceModern;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Provider;
import org.gradle.internal.reflect.Instantiator;

import java.io.File;
import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * GradleCrutchService:
 * <p>
 * <p>While migrating from gradle 5.X (custom internal version) to 8.X (mainstream version),
 * there are API incompatibilities which need to compile in the custom version,
 * but still survive when running in the mainstream version.
 * <p>
 * <p>This class is where we hide the incompatibilities, to limp the migration along.
 * <p>Once migration is complete, this interface, and all references to it should be removed.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 27/10/2024 @ 12:31 a.m.
 */
public interface GradleCrossVersionService {

    static GradleCrossVersionService getService(Gradle gradle) {
        final ServiceLoader<GradleCrossVersionService> services = ServiceLoader.load(GradleCrossVersionService.class);
        for (GradleCrossVersionService service : services) {
            // somebody registered a service. let them win.
            // it's hacky and lazy to do this, but this whole service is a temporary migration crutch
            service.init(gradle);
            return service;
        }
        GradleCrossVersionService result = new GradleCrossVersionServiceModern();
        try {

            final Class<?> legacy = GradleCrossVersionService.class.getClassLoader().loadClass("net.wti.gradle.migrate.GradleCrossVersionServiceLegacy");
            if (legacy != null) {
                result = (GradleCrossVersionService) legacy.getConstructor().newInstance();
            }
        } catch (Exception ignored) { }
        result.init(gradle);
        return result;
    }

    <T> AbstractPublishArtifact publishArtifact(String name, T task, Function<T, File> toFile);
    <T> AbstractPublishArtifact publishArtifactSources(String name, T task, Function<T, File> toFile);
    PublishArtifact lazyArtifact(Provider<?> task, String version);
    ProjectInternal findProject(ProjectFinder projectFinder, String path);
    <T> NamedDomainObjectList<T> namedDomainList(Class<T> cls, Instantiator instantiator);
    default void init(Gradle gradle) {
    }
}

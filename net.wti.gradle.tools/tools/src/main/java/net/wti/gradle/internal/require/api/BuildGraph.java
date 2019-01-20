package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.require.api.XapiRequire;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.SourceMeta;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.spi.GradleServiceFinder;
import org.gradle.api.*;

import java.util.Set;

/**
 * Represents a partially realized build graph of the entire gradle build.
 *
 * A single instance is attached to the root project, and filled-in / shared as lazily as possible by all projects.
 *
 * Whenever a {@link XapiSchema} is configured, it will register all possible nodes in the build graph.
 * Whenever a {@link XapiRequire} is used, it will realize the registered nodes as it wires up dependencies.
 *
 * Whenever there is a sourceSet directory present, the associated {@link SourceMeta} will always be created
 * (unless -Pxapi.platform=specificPlatform, in which case, only specificPlatform and its ancestors will be created).
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/31/18 @ 3:25 AM.
 */
public interface BuildGraph {

    String EXT_NAME = "xapiBuild";

    static BuildGraph findBuildGraph(Project project) {
        final GradleService service = GradleServiceFinder.getService(project);
        return service.buildOnce(
            BuildGraph.class, EXT_NAME, p-> service.createBuildGraph());
    }

    NamedDomainObjectProvider<ProjectGraph> project(Object path);

    default ProjectGraph getProject(Object path) {
        return project(path).get();
    }

    Set<ProjectGraph> realizedProjects();
    Set<String> registeredProjects();

    ProjectView rootProject();

    // Consider composite-mapping a bit here...

    // The schema should be used to register potential configurations / sourcesets
    // Requires / task activations should trigger configuration realization
}

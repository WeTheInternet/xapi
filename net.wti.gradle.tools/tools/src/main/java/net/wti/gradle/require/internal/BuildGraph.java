package net.wti.gradle.require.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.require.api.DependencyKey;
import net.wti.gradle.require.api.XapiRequire;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.SourceMeta;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.spi.GradleServiceFinder;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.*;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.internal.reflect.Instantiator;

import java.io.File;
import java.util.Map;
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
            BuildGraph.class, GradleService.EXT_NAME, p-> service.buildGraph());
    }

    NamedDomainObjectProvider<ProjectGraph> project(Object path);

    Set<ProjectGraph> realizedProjects();
    Set<String> registeredProjects();

    ProjectView rootProject();
    ProjectView getProject();

    interface ProjectGraph extends Named {
        BuildGraph root();
        ProjectView project();
        NamedDomainObjectContainer<PlatformGraph> platforms();

        default PlatformGraph platform(CharSequence name) {
            return platforms().maybeCreate(name.toString());
        }
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
    }

    interface PlatformGraph extends Named {
        ProjectGraph project();
        PlatformGraph parent();
        ArchiveGraph archive(Object name);
        Set<ArchiveGraph> realize();
        Set<ArchiveGraph> realizedArchives();
        Set<ArchiveRequest> incoming();
        Set<ArchiveRequest> outgoing();

        default boolean isSelectable() {
            return project().isSelectable(this);
        }

        default boolean matches(String requested) {
            if (requested.equals(getName())) {
                return true;
            }
            final PlatformGraph parent = parent();
            if (parent == null) {
                return false;
            }
            return parent.matches(requested);
        }
    }

    interface ArchiveGraph extends Named {
        PlatformGraph platform();

        default ProjectGraph project() {
            return platform().project();
        }

        ArchiveRequest request(ArchiveGraph other, ArchiveRequestType type);

        /**
         * @return group:name:version / whatever we need to create external dependencies.
         *
         * If you are creating an archive graph from external module dependencies,
         * then this map contains either group:name:version:changing:etc,
         * or is structured as a "deep map":
         * `{composite: [{g:n:v:c:etc}, {...}]`
         *
         * When an archive graph is being created from project sources,
         * then this map will have `{external:false, g:n:v:etc:...}`,
         * and it will control the publishing destination / external lookup of this graph node.
         *
         */
        Map<DependencyKey, ?> id();

        /**
         * @return The location of the source set directory;
         * If you have a project `projName`, a platform of `plat` and archive type of `arch`,
         * then the conventional value of this property is roughly:
         * `$rootDir/projName/src/platArch`
         */
        File srcRoot();

        default boolean srcExists() {
            return srcRoot().exists();
        }


        Set<ArchiveRequest> getIncoming();
        Set<ArchiveRequest> getOutgoing();

        default boolean hasIncoming() {
            return getIncoming().stream().anyMatch(ArchiveRequest::isSelectable);
        }
        // this node is used as a dependency
        boolean hasOutgoing();

        default boolean realized() {
            return srcExists() || hasIncoming() || hasOutgoing();
        }

        default boolean isSelectable() {
            return platform().isSelectable();
        }
    }
    interface ArchiveRequest extends Named {
        ArchiveGraph from();
        ArchiveGraph to();
        ArchiveRequestType type();

        @Override
        default String getName() {
            return from().getName() + type().symbol + to().getName();
        }

        default boolean isSelectable() {
            return from().isSelectable() || to().isSelectable();
        }
    }

    enum ArchiveRequestType {
        /**
         * Process sources/classpath into new source files
         */
        TRANSPILE("=>"),
        /**
         * Process sources/classpath into binary output files
         */
        COMPILE("->"),
        /**
         * A development runtime (will include sources / private tools).
         * Will prefer class directories on unix machines, and jars/zips on windows.
         * Will include sources and tools like runtime injection support.
         *
         */
        RUNTIME_DEV("==>"),
        /**
         * A strict production runtime (minified archives / no extras).
         * Will use jars/zips unless explicitly configured otherwise.
         * Will avoid tools like runtime injection support unless explicitly added.
         * May also attempt AoT compilation if prod jvm args are supplied.
         */
        RUNTIME_PROD("-->"),
        /**
         * A whole-world "shadow archive" for a given platform.
         */
        DISTRIBUTION("*")
        ;
        private final String symbol;

        ArchiveRequestType(String symbol) {
            this.symbol = symbol;
        }
    }

    // Consider composite-mapping a bit here...

    // The schema should be used to register potential configurations / sourcesets
    // Requires / task activations should trigger configuration realization
}

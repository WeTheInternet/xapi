package net.wti.gradle.schema.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.system.plugin.XapiBasePlugin;
import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.internal.reflect.Instantiator;

import javax.inject.Inject;
import java.util.function.BiConsumer;

/**
 * This plugin configures the xapi schema,
 * which determines what sourceSets and variants will be created,
 * and the relationship between those sourceSets.
 *
 * This plugin should only be applied to root projects,
 * and only serves to define the set of platforms (variants)
 * and the set of archives within each platform.
 *
 * This plugin exposes the extension dsl xapiSchema {},
 * backed by {@link XapiSchema}, which will be frozen as soon as it is read from.
 *
 * This means that you should always setup your schema as early as possible,
 * in your root buildscript, and you should not expect to manipulate it dynamically.
 *
 * We may eventually add support for patching the schema on a per-project level,
 * but for now, we want to force a homogenous environment,
 * so we can wait until we have a strong use case to bother multiplexing it.
 *
 *  TODO: consider XapiSchemaSettingsPlugin, which could be used
 *  to create gradle subprojects to back a given variant.
 *
 *  Hm.  Also consider generating a build-local, optional init/ide script from the schema,
 *  which adds the xapiRequire dsl.  We can also create a class that goes on the classpath to match,
 *  via a buildSrc-y plugin.  It would likely be wise to treat your schema as a standalone gradle build,
 *  and just composite it, pre-built, into place.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/26/18 @ 2:48 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public class XapiSchemaPlugin implements Plugin<Project> {

    public static final Attribute<String> ATTR_ARTIFACT_TYPE = Attribute.of("moduleType", String.class);
    public static final Attribute<String> ATTR_PLATFORM_TYPE = Attribute.of("platformType", String.class);

    private final Instantiator instantiator;

    @Inject
    public XapiSchemaPlugin(Instantiator instantiator) {
        this.instantiator = instantiator;
    }

    @Override
    public void apply(Project project) {
        // eagerly initialize the buildgraph, so it can hook up lifecycle events asap
        BuildGraph.findBuildGraph(project);
        final PluginContainer plugins = project.getPlugins();
        plugins.apply(XapiBasePlugin.class);

        final ProjectView view = ProjectView.fromProject(project);
        boolean isRoot = view == schemaRootProject(view);
        BuildGraph graph = view.getBuildGraph();
        if (isRoot) {
            // signal that the root schema.xapi should be parsed, and a SchemaMap built.

        }
        finalize(view, graph);

        if (isRoot) {
            // do not attempt to resolve sourcesets and module tasks in the schema root project... yet.

            return;
        }
        if ("true".equals(System.getProperty("idea.resolveSourceSetDependencies"))) {
            // idea is trying to resolve dependencies... make sure we realize everything!
            view.getProjectGraph().platforms().all(plat -> {
                plat.archives().all(mod -> {
                    graph.whenReady(ReadyState.FINISHED, done->{
                        mod.getSource();
                        mod.getTasks().realizeAll();
                    });
                });
            });
        }
        // DO NOT ADD CODE HERE WITHOUT CONSIDERING THE EARLY RETURN STATEMENT ABOVE!
    }

    @SuppressWarnings({"ConstantConditions"}) // we null check the input to the method that can return null
    private XapiSchema finalize(ProjectView view, BuildGraph graph) {
        final Object limiter = view.findProperty("xapi.platform");
        final XapiSchema schema = view.getSchema();

        BiConsumer<PlatformConfig, ArchiveConfig> configure = (platformConfig, archiveConfig) -> {
            graph.project(view.getPath()).configure(proj -> {
                proj.getOrRegister(platformConfig.getName()).configure(
                    platformGraph -> {
                        // These methods are just here to make sure everything is registered,
                        // for now the only useful thing we can do here is throw in some assertions,
                        // to help catch if anything ever goes wonky w.r.t key collisions
                        assert platformGraph.config() == platformConfig;
                        platformGraph.getOrRegister(archiveConfig.getName()).configure(
                            archiveGraph -> {
                                assert archiveGraph.config() == archiveConfig;
                            }
                        );
                    }
                );
            });
        };
        // TODO: find a non-recursion-sick way to defer the eager realization below until the build graph is realized.
        if (limiter == null) {
            // If there was no platform property, we will eagerly realize all containers.
            schema.getPlatforms().all(platformConfig ->
                platformConfig.getArchives().all(archConfig->{
                    configure.accept(platformConfig, archConfig);
                })
            );
        } else {
            // If there is a platform property, we'll want to limit the realization of platform containers.
            // Include only the named platform and all parent platforms thereof.
            String limit = GradleCoerce.unwrapString(limiter);
            for (
                PlatformConfig next = schema.getPlatforms().getByName(limit);
                next != null;
                next = next.getParent() ) {
                final PlatformConfig current = next;
                next.getArchives().all(archConfig-> configure.accept(current, archConfig));
            }
        }
        return schema;
    }

    private static String schemaRootPath(ProjectView project) {
        String path = (String) project.findProperty("xapi.schema.root");
        if (path == null) {
            if (project.findProject(":xapi-schema") != null) {
                return ":xapi-schema";
            }
            if (project.findProject(":schema") != null) {
                return ":schema";
            }
            return ":";
        }
        return path.startsWith(":") ? path : ":" + path;
    }

    public static ProjectView schemaRootProject(ProjectView project) {
        String path = schemaRootPath(project);
        final ProjectView root = path == null ? project.getRootProject() : project.findProject(path);
        if (project != root) {
            root.ensureEvaluated();
            root.getPlugins().apply(XapiSchemaPlugin.class);
            GradleService.buildOnce(root, XapiSchema.EXT_NAME, XapiSchema::new);
        }
        return root;
    }
}

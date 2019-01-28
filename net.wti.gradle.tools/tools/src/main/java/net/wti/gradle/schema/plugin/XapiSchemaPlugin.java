package net.wti.gradle.schema.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.require.plugin.XapiRequirePlugin;
import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
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

    public static final String PROP_SCHEMA_APPLIES_JAVA = "xapi.apply.java";
    public static final String PROP_SCHEMA_APPLIES_JAVA_LIBRARY = "xapi.apply.java-library";

    public static final Attribute<String> ATTR_ARTIFACT_TYPE = Attribute.of("moduleType", String.class);
    public static final Attribute<String> ATTR_PLATFORM_TYPE = Attribute.of("platformType", String.class);

    private final Instantiator instantiator;
    private Gradle gradle;
    private boolean withJavaPlugin, withJavaLibraryPlugin;

    @Inject
    public XapiSchemaPlugin(Instantiator instantiator) {
        this.instantiator = instantiator;
    }

    @Override
    public void apply(Project project) {
        gradle = project.getGradle();
        final PluginContainer plugins = project.getPlugins();
        plugins.apply(JavaBasePlugin.class);

        // This block below should go into a (new) XapiBasePlugin
        // If we're going to interact w/ java and java-library, they need to go first,
        // since they don't always maybeCreate() something we want to interop w/.
        if ("true".equals(project.findProperty(PROP_SCHEMA_APPLIES_JAVA_LIBRARY))) {
            plugins.apply(JavaLibraryPlugin.class);
            withJavaLibraryPlugin = true;
        } else if ("true".equals(project.findProperty(PROP_SCHEMA_APPLIES_JAVA))) {
            plugins.apply(JavaPlugin.class);
            withJavaPlugin = true;
        } else {
            // user did not explicitly request either java of java-library integration.  Setup boobytraps:
            boolean[] locked = {false};
            boolean has = !plugins.withType(JavaPlugin.class, p->{
                if (locked[0]) {
                    throw new IllegalStateException("You must apply the java plugin before xapi-schema, or set gradle property -P" + PROP_SCHEMA_APPLIES_JAVA +"=true");
                }
                withJavaPlugin = true;
            }).isEmpty();
            has |= !plugins.withType(JavaLibraryPlugin.class, p->{
                if (locked[0]) {
                    throw new IllegalStateException("You must apply the java-library plugin before xapi-schema, or set gradle property -P" + PROP_SCHEMA_APPLIES_JAVA_LIBRARY +"=true");
                }
                withJavaLibraryPlugin = true;
            }).isEmpty();
            if (!has) {
                // User has not added any java plugin yet.
                // In the future, we'll likely replace the guts of JavaPlugin here.
                // For now, we'll just apply it ourselves.
                plugins.apply(JavaPlugin.class);
            }
            locked[0] = true;
        }

        final ProjectView self = ProjectView.fromProject(project);
        final ProjectView root = schemaRootProject(self);

        final XapiSchema schema = self.getSchema();

        if (self == root) {
            // When we are the schema root project,
            // we may want to detect and interact with the java-platform component.
            // Specifically, we want to make it simple to have the schema read and expose
            // the java-platform's dependency information to all xapiRequire instances.
            // Preferably, we also generate code to give typesafe "suggestions from the platform".
            // Making this a read-write relationship, it should be possible to declare the same
            // information in dependencies {} as xapiRequire {}, and both generate the same bom / behavior / artifacts.
        } else {
            // Ok, we have our root schema, now use it to apply any necessary configurations / sourceSets.
            BuildGraph graph = self.getBuildGraph();
            finalize(project, schema, graph);
        }
    }

    @SuppressWarnings({"ConstantConditions"}) // we null check the input to the method that can return null
    private void finalize(Project project, XapiSchema schema, BuildGraph graph) {
        project.getPlugins().withId("java-base", ignored-> {
            // The java base plugin was applied.  Create a xapiRequire extension.
            project.getPlugins().apply(XapiRequirePlugin.class);
        });
        Object limiter = project.findProperty("xapi.platform");

        BiConsumer<PlatformConfig, ArchiveConfig> configure = (platformConfig, archiveConfig) -> {
            graph.project(project.getPath()).configure(proj -> {
                proj.getOrRegister(platformConfig.getName()).configure(
                    platformGraph -> {
                        // These methods are just here to make sure everything is registered,
                        // for now the only useful thing we can do here is throw in some assertions,
                        // to help catch if anything ever goes wonky w.r.t cross-project config objects.
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
                platformConfig.getAllArchives().all(archConfig->{
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
                next.getAllArchives().all(archConfig-> configure.accept(current, archConfig));
            }
        }
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
            GradleService.buildOnce(root, XapiSchema.EXT_NAME, XapiSchema::new);
        }
        return root;
    }
}

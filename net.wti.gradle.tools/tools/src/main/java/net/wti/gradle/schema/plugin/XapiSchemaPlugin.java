package net.wti.gradle.schema.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.require.plugin.XapiRequirePlugin;
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
import org.gradle.internal.reflect.Instantiator;

import javax.inject.Inject;

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
    public static final String PROP_SCHEMA_APPLIES_JAVA_PLATFORM = "xapi.apply.java-platform";

    public static final Attribute<String> ATTR_USAGE = Attribute.of("usage", String.class);
    public static final Attribute<String> ATTR_ARTIFACT_TYPE = Attribute.of("artifactType", String.class);
    public static final Attribute<String> ATTR_PLATFORM_TYPE = Attribute.of("platformType", String.class);

    private final Instantiator instantiator;
    private Gradle gradle;

    @Inject
    public XapiSchemaPlugin(Instantiator instantiator) {
        this.instantiator = instantiator;
    }

    @Override
    public void apply(Project project) {
        gradle = project.getGradle();
        project.getPlugins().apply(JavaBasePlugin.class);
        // If we're going to interact w/ java and java-library, they need to go first,
        // since they don't always maybeCreate() something we want to interop w/.
        if ("true".equals(project.findProperty(PROP_SCHEMA_APPLIES_JAVA))) {
            project.getPlugins().apply(JavaPlugin.class);
        }
        if ("true".equals(project.findProperty(PROP_SCHEMA_APPLIES_JAVA_LIBRARY))) {
            project.getPlugins().apply(JavaLibraryPlugin.class);
        }
        final ProjectView self = ProjectView.fromProject(project);
        final ProjectView root = schemaRootProject(self);

        final XapiSchema schema = self.getSchema();

        if (self == root) {
            // When we are the schema root project,
            // we may want to detect and interact with the java-platform plugin.
            // Specifically, we want to make it simple to have the schema read and expose
            // the platform-plugin's configurations to all xapiRequire instances.
            // Preferably, we also generate code to give typesafe "suggestions from the platform".

        } else {
            // Ok, we have our root schema, now use it to apply any necessary configurations / sourceSets.

            BuildGraph graph = self.getBuildGraph();
//            ProjectGraph proj = graph.getProject(project.getPath());
//
//            schema.getPlatforms().configureEach(platform -> {
//                String name = platform.getName();
//                final PlatformGraph platGraph = proj.platform(name);
//                platform.getArchives().configureEach(archive -> {
//                    final ArchiveGraph archGraph = platGraph.archive(archive.getName());
//                    if (archGraph.srcExists()) {
//
//                    }
//                });
//                File src = project.file("src/" + name);
//                if (src.isDirectory()) {
//                    // add a variant to the system
//                    addPlatform(project, schema, graph, (PlatformConfigInternal) platform);
//                }
//            });

            finalize(project, schema, graph);

        }
    }

    @SuppressWarnings("unchecked")
    private void finalize(Project project, XapiSchema schema, BuildGraph graph) {
        project.getPlugins().withId("java-base", ignored-> {
            // The java base plugin was applied.  Create a xapiRegister extension.
            project.getPlugins().apply(XapiRequirePlugin.class);
        });
        Object limiter = project.findProperty("xapi.platform");
        if (limiter == null) {
            // If there was no platform property, we will eagerly realize all containers.
            schema.getPlatforms().all(platformConfig ->
                platformConfig.getArchives().all(ignored->{

                })
            );
        } else {
            // If there is a platform property, we'll want to limit the realization of platform containers.
            String limit = GradleCoerce.unwrapString(limiter);
            PlatformConfig allowed = schema.getPlatforms().getByName(limit);
            for (;allowed != null; allowed = allowed.getParent()) {
                allowed.getArchives().all(ignored->{

                });
            }
        }
    }

//    @SuppressWarnings("unchecked")
//    private void addPlatform(
//        Project project_,
//        XapiSchema schema,
//        BuildGraph graph,
//        PlatformConfigInternal platform
//    ) {
//        // TODO need to consider the platform's archives as well.  For now, we're ignoring them.
//        ArchiveConfigContainerInternal archives = platform.getArchives();
//
//        ProjectView project = ProjectView.fromProject(project_);
//
//
//        final SourceSetContainer srcs = project.getSourceSets();
//        // The main sourceSet for this platform.
//        final PlatformConfigInternal mainPlatform = schema.findPlatform("main");
//        final ArchiveConfig mainAch = archives.maybeCreate(mainPlatform.sourceName(platform.isTest() ? "test" : "main"));
//        final SourceMeta mainMeta = mainPlatform.sourceFor(srcs, mainAch);
//        final DependencyHandler deps = project.getDependencies();
//        final SourceMeta meta = platform.sourceFor(srcs, archives.maybeCreate("main"));
//        final SourceSet src = meta.getSrc();
//        project.getTasks().named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, t->{
//            t.dependsOn(src.getCompileJavaTaskName());
//            t.dependsOn(src.getProcessResourcesTaskName());
//        });
//
//        // Need to setup the various configurations...
//        String name = platform.getName();
//
//        final PlatformGraph platGraph = project.getProjectGraph().platform(name);
//
//        final ConfigurationContainer configs = project.getConfigurations();
//        final Configuration compile = configs.getByName(src.getCompileConfigurationName());
//
//
//
//        final ArchiveConfig[] all = archives.toArray(new ArchiveConfig[0]);
//        for (ArchiveConfig archive : all) {
//            final Set<String> needed = archive.required();
//            final SourceMeta myMeta = platform.sourceFor(srcs, archive);
//            String myCfg = platform.configurationName(archive);
//            final SourceSet source = myMeta.getSrc();
//            final Configuration cfg = configs.maybeCreate(myCfg);
//
////            if (archive.isSourceAllowed()) {
////                addSourceConfiguration(project, schema, platform, archive, myMeta);
////            }
//
//            deps.add(
//                source.getCompileConfigurationName(),
//                cfg
//            );
//            for (String need : needed) {
//                boolean only = need.endsWith("*");
//                if (only) {
//                    need = need.substring(0, need.length()-1);
//                }
//                if (!project_.file("src/" + need).exists()) {
//                    if (filterMissing()) {
//                        continue;
//                    }
//                }
//                PlatformConfig conf = schema.findPlatform(need);
//                if (conf == null) {
//                    conf = mainPlatform;
//                }
//                need = GUtil.toLowerCamelCase(need.replace(conf.getName(), ""));
//                ArchiveConfig neededArchive = conf.getArchives().findByName(need);
//                if (neededArchive == null) {
//                    neededArchive = archives.findByName(need);
//                }
//                if (neededArchive == null) {
//                    neededArchive = conf.getArchives().create(need);
//                }
//
//                final SourceMeta neededSrc = platform.sourceFor(srcs, neededArchive);
//
//                if (only) {
//                    deps.add(
//                        source.getCompileOnlyConfigurationName(),
//                        neededSrc.depend(deps)
//                    );
//                } else {
//                    deps.add(
//                        cfg.getName(),
//                        neededSrc.depend(deps)
//                    );
//                }
//            }
//
//        }
//
//    }
//
//    private void addSourceConfiguration(
//        ProjectView project,
//        XapiSchema schema,
//        PlatformConfigInternal platform,
//        ArchiveConfig archive,
//        SourceMeta meta
//    ) {
//        String name = platform.sourceName(archive) + "Source";
//        ConfigurationContainer configs = project.getConfigurations();
//        if (configs.findByName(name) != null) {
//            return;
//        }
//
//        String taskName = name + "Jar";
//        final TaskContainer tasks = project.getTasks();
//        ProviderFactory providers = project.getProviders();
//        final Configuration config = configs.maybeCreate(name);
//
//        final TaskProvider<Jar> jarTask = tasks.register(taskName, Jar.class, jar -> {
//            jar.from(meta.getSrc().getAllSource());
//            if (schema.getArchives().isWithCoordinate()) {
//                assert !schema.getArchives().isWithClassifier() : "Archive container cannot use both coordinate and classifier: " + schema.getArchives();
//                jar.getArchiveAppendix().set("sources");
//            } else {
//                jar.getArchiveClassifier().set("sources");
//            }
//            jar.getExtensions().add(SourceMeta.EXT_NAME, meta);
//        });
//
//        config.getOutgoing().variants(variants->{
//            final ConfigurationVariant variant = variants.maybeCreate("sources");
//            variant.attributes(attrs->{
//                attrs.attribute(ATTR_ARTIFACT_TYPE, "sources");
//            });
//            final LazyPublishArtifact art = new LazyPublishArtifact(
//                providers.provider(()->{
//                    // Task was resolved, lets make sure there's also archives present for the individual source directories
//                    for (File srcDir : meta.getSrc().getAllSource().getSrcDirs()) {
//                        final Provider<File> provider = providers.provider(() -> srcDir);
//                        LazyPublishArtifact src = new LazyPublishArtifact(provider);
//                        variant.artifact(src);
//                    }
//                    return jarTask.get();
//                })
//            );
//            config.getOutgoing().artifact(art);
//        });
//    }

    private boolean filterMissing() {
        return true;
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

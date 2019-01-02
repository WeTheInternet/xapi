package net.wti.gradle.schema.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.XapiLibrary;
import net.wti.gradle.internal.api.XapiPlatform;
import net.wti.gradle.require.plugin.XapiRequirePlugin;
import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.ArchiveConfigContainerInternal;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.internal.SourceMeta;
import net.wti.gradle.schema.tasks.XapiReport;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.util.GUtil;
import xapi.gradle.java.Java;

import javax.inject.Inject;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

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

    Attribute<String> ATTR_USAGE = Attribute.of("usage", String.class);
    Attribute<String> ATTR_ARTIFACT_TYPE = Attribute.of("artifactType", String.class);
    Attribute<String> ATTR_PLATFORM_TYPE = Attribute.of("platformType", String.class);

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
        if ("true".equals(project.findProperty(PROP_SCHEMA_APPLIES_JAVA))) {
            project.getPlugins().apply(JavaPlugin.class);
        }
        if ("true".equals(project.findProperty(PROP_SCHEMA_APPLIES_JAVA_LIBRARY))) {
            project.getPlugins().apply(JavaLibraryPlugin.class);
        }
        final String schemaRoot = schemaRootPath(project);
        final Project root = project.project(schemaRoot);
        ProjectView view = ProjectView.fromProject(project);

        // This stupid assert is here to do nothing, but still give a nice constructor reference,
        // for better IDE navigation (decorated extensions.create instantiation below)
        //noinspection all
        assert 1 == 1 || null == new XapiSchema(view);
        final XapiSchema schema = project.getExtensions().create(XapiSchema.EXT_NAME, XapiSchema.class, view);

        if (project != root) {
            // TODO: only allow xapiSchema {} _before_ xapiRequire {} is accessed / used in any way.

            // If we aren't in the root project, then we should read from the root project,
            // and apply those settings to ourselves.
            XapiSchema rootSchema = (XapiSchema) root.getExtensions().findByName(XapiSchema.EXT_NAME);
            if (rootSchema == null) {
                // help the user out with a useful error
                throw new GradleException("You must apply `plugins { id 'xapi-schema' }` in " + schemaRoot +
                    (":".equals(schemaRoot) ? "" : " and call `evaluationDependsOn '" + schemaRoot + "'` " +
                        "at/near the top of " + project.getBuildFile().getAbsolutePath()));
            }

            schema.initialize(rootSchema);

            // Ok, we have our root schema, now use it to apply any necessary configurations / sourceSets.
            final ArchiveConfigContainerInternal archives = schema.getArchives();

            XapiLibrary lib = new XapiLibrary(schema, project.getObjects(), project.getProviders(), instantiator);
            schema.getPlatforms().configureEach(platform -> {
                String name = platform.getName();
                File src = project.file("src/" + name);
                if (src.isDirectory()) {
                    // add a variant to the system
                    addPlatform(project, lib, schema, platform, archives);
                }
            });
            finalize(project, schema, lib);

        }
    }

    @SuppressWarnings("unchecked")
    private void finalize(Project project, XapiSchema schema, XapiLibrary lib) {
        project.getTasks().register(XapiReport.TASK_NAME, XapiReport.class, report ->
            report.record(schema, lib)
        );
        project.getPlugins().withId("java-base", ignored-> {
            // The java base plugin was applied.  Create a xapiRegister extension.
            project.getPlugins().apply(XapiRequirePlugin.class);
        });
//        ((TaskExecutionGraphInternal)project.getGradle().getTaskGraph()).populate();
//        final boolean exists = gradle.getTaskGraph().hasTask(project.getPath() + ":compileJava");
//        project.getLogger().quiet("\n\n\nMEOW{}", exists);
    }

    @SuppressWarnings("unchecked")
    private void addPlatform(
        Project project,
        XapiLibrary lib,
        XapiSchema schema,
        PlatformConfig platform_,
        ArchiveConfigContainerInternal archives
    ) {
        final PlatformConfigInternal platform = (PlatformConfigInternal) platform_;
        // TODO need to consider the platform's archives as well.  For now, we're ignoring them.
        String name = platform.getName();
        final XapiPlatform plat = lib.getPlatform(name);

        final PlatformConfigInternal parent = platform.getParent();
        boolean needsSources = platform.isRequireSource();

        final SourceSetContainer srcs = Java.sources(project);
        // The main sourceSet for this platform.
        final PlatformConfigInternal mainPlatform = schema.findPlatform("main");
        final ArchiveConfig mainAch = archives.maybeCreate(mainPlatform.sourceName(platform.isTest() ? "test" : "main"));
        final SourceMeta mainMeta = mainPlatform.sourceFor(srcs, mainAch);
        final DependencyHandler deps = project.getDependencies();
        final SourceMeta meta = platform.sourceFor(srcs, archives.maybeCreate("main"));
        final SourceSet src = meta.getSrc();
        project.getTasks().named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, t->{
            t.dependsOn(src.getCompileJavaTaskName());
            t.dependsOn(src.getProcessResourcesTaskName());
        });

        // Need to setup the various configurations...

        final ConfigurationContainer configs = project.getConfigurations();
        final Configuration myGlobal = configs.maybeCreate(platform.getName());
        final Configuration assembled = configs.maybeCreate(platform.getName() + "Assembled");
        final Configuration myApi = configs.maybeCreate(meta.getApiConfigurationName());
        final Configuration myImpl = configs.maybeCreate(meta.getImplementationConfigurationName());
        final Configuration compile = configs.getByName(src.getCompileConfigurationName());
        myImpl.extendsFrom(myApi);
        myApi.extendsFrom(myGlobal);
        compile.extendsFrom(myImpl);
        assembled.extendsFrom(myApi);
        deps.add(
            assembled.getName(), src.getOutput()
        );

        if (needsSources) {
            // Must add our own source, as well as the source of all dependencies to our classpath.
            final SourceDirectorySet mainOut = src.getAllSource();

            deps.add(
                myApi.getName(), deps.create( mainOut.getSourceDirectories() )
            );
            Set<Dependency> extras = new LinkedHashSet<>();
            compile.getAllDependencies().configureEach(dep->{
                if (dep instanceof ProjectDependency) {
                    ProjectDependency projDep = (ProjectDependency) dep;
                    String target = projDep.getTargetConfiguration();

                    if (target == null) {
                        // default configuration.  Convert this to "source for default configuration"
                        target = "mainSource";
                    } else if (!target.endsWith("Source")){
                        if (target.contains("Assembled")) {
                            target = target.replace("Assembled", "Source");
                        } else {
                            target = target + "Source";
                        }
                    }
                    extras.add(deps.project(GUtil.map(
                        "path", projDep.getDependencyProject().getPath(),
                                "configuration", target
                    )));

                } else if (dep instanceof ExternalDependency) {
                    // an external dependency...  we'll want to add extra artifacts to request.
                    // Need to route this through a lazily-loaded detached configuration,
                    // or a firm 1:1 name-mapping (i.e. you define when to use -sources name suffix,
                    // versus a -source classifier suffix.

                } else if (dep instanceof FileCollectionDependency) {
                    final FileCollection files = ((FileCollectionDependency) dep).getFiles();
                    if (files != mainOut.getSourceDirectories()) {
                        if (dep instanceof ExtensionAware) {
                            final Object srcSet = ((ExtensionAware) dep).getExtensions().findByName(SourceMeta.EXT_NAME);
                            if (srcSet != null) {
                                FileCollection srcDirs = ((SourceMeta) srcSet).getSrc().getAllSource().getSourceDirectories();
                                if (filterMissing()) {
                                    srcDirs = srcDirs.filter(File::exists);
                                }
                                extras.add(deps.create( srcDirs ));
                            }
                        }
                    }
                }
            });
            gradle.getTaskGraph().whenReady(graph->{
                if (!extras.isEmpty()) {
                    final Dependency[] toAdd = extras.toArray(new Dependency[0]);
                    extras.clear();
                    for (Dependency dependency : toAdd) {
                        deps.add(myApi.getName(), dependency);
                    }
                }
                if (!extras.isEmpty()) {
                    project.getLogger().error("Found extra dependencies after adding extra dependencies: {}", extras);
                }
            });
        }
        if (!platform.isRoot() && src != mainMeta.getSrc()) {
            deps.add(myApi.getName(), mainMeta.depend(deps));
        }

        final ArchiveConfig[] all = archives.toArray(new ArchiveConfig[0]);
        for (ArchiveConfig archive : all) {
            final Set<String> needed = archive.required();
            final SourceMeta myMeta = platform.sourceFor(srcs, archive);
            String myCfg = platform.configurationName(archive);
            final SourceSet source = myMeta.getSrc();
            final Configuration cfg = configs.maybeCreate(myCfg);

            if (archive.isSourceAllowed()) {
                addSourceConfiguration(project, schema, platform, archive, myMeta);
            }

            deps.add(
                source.getCompileConfigurationName(),
                cfg
            );
            for (String need : needed) {
                boolean only = need.endsWith("*");
                if (only) {
                    need = need.substring(0, need.length()-1);
                }
                if (!project.file("src/" + need).exists()) {
                    if (filterMissing()) {
                        continue;
                    }
                }
                PlatformConfig conf = schema.findPlatform(need);
                if (conf == null) {
                    conf = mainPlatform;
                }
                need = GUtil.toLowerCamelCase(need.replace(conf.getName(), ""));
                ArchiveConfig neededArchive = conf.getArchives().findByName(need);
                if (neededArchive == null) {
                    neededArchive = archives.findByName(need);
                }
                if (neededArchive == null) {
                    neededArchive = conf.getArchives().create(need);
                }

                final SourceMeta neededSrc = platform.sourceFor(srcs, neededArchive);

                if (only) {
                    deps.add(
                        source.getCompileOnlyConfigurationName(),
                        neededSrc.depend(deps)
                    );
                } else {
                    deps.add(
                        cfg.getName(),
                        neededSrc.depend(deps)
                    );
                }
            }

        }

    }

    private void addSourceConfiguration(
        Project project,
        XapiSchema schema,
        PlatformConfigInternal platform,
        ArchiveConfig archive,
        SourceMeta meta
    ) {
        String name = platform.sourceName(archive) + "Source";
        ConfigurationContainer configs = project.getConfigurations();
        if (configs.findByName(name) != null) {
            return;
        }

        String taskName = name + "Jar";
        final TaskContainer tasks = project.getTasks();
        ProviderFactory providers = project.getProviders();
        final Configuration config = configs.maybeCreate(name);

        final TaskProvider<Jar> jarTask = tasks.register(taskName, Jar.class, jar -> {
            jar.from(meta.getSrc().getAllSource());
            if (schema.getArchives().isWithCoordinate()) {
                assert !schema.getArchives().isWithClassifier() : "Archive container cannot use both coordinate and classifier: " + schema.getArchives();
                jar.getArchiveAppendix().set("sources");
            } else {
                jar.getArchiveClassifier().set("sources");
            }
            jar.getExtensions().add(SourceMeta.EXT_NAME, meta);
        });

        config.getOutgoing().variants(variants->{
            final ConfigurationVariant variant = variants.maybeCreate("sources");
            variant.attributes(attrs->{
                attrs.attribute(ATTR_ARTIFACT_TYPE, "sources");
            });
            final LazyPublishArtifact art = new LazyPublishArtifact(
                providers.provider(()->{
                    // Task was resolved, lets make sure there's also archives present for the individual source directories
                    for (File srcDir : meta.getSrc().getAllSource().getSrcDirs()) {
                        final Provider<File> provider = providers.provider(() -> srcDir);
                        LazyPublishArtifact src = new LazyPublishArtifact(provider);
                        variant.artifact(src);
                    }
                    return jarTask.get();
                })
            );
            config.getOutgoing().artifact(art);
        });
    }

    private boolean filterMissing() {
        return true;
    }

    protected String schemaRootPath(Project project) {
        String path = (String) project.findProperty("xapi.schema.root");
        if (path == null) {
            if (project.findProject(":schema") != null) {
                path = ":schema";
            }
        }
        return path == null ? ":" : path.startsWith(":") ? path : ":" + path;
    }

    public static Project schemaRootProject(Project project) {
        String path = (String) project.findProperty("xapi.schema.root");
        if (path == null) {
            project = project.getRootProject();
            final XapiSchemaPlugin plugin = project.getPlugins().apply(XapiSchemaPlugin.class);
            path = plugin.schemaRootPath(project);
        } else {
            project = project.project(path);
            project.getPlugins().apply(XapiSchemaPlugin.class);
        }
        return project.project(path);
    }
}

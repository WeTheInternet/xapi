package net.wti.gradle.parser.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.*;
import net.wti.gradle.schema.api.SchemaIndexReader;
import net.wti.gradle.schema.index.IndexBackedSchemaMap;
import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.map.*;
import net.wti.gradle.schema.api.SchemaDependency;
import net.wti.gradle.schema.api.SchemaModule;
import net.wti.gradle.schema.api.SchemaProject;
import net.wti.gradle.schema.spi.SchemaProperties;
import net.wti.gradle.settings.ProjectDescriptorView;
import net.wti.gradle.system.plugin.XapiBasePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.SourceSet;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.itr.MappedIterable;
import xapi.gradle.fu.LazyString;

import java.io.File;

import static net.wti.gradle.internal.require.api.ArchiveGraph.toConfigName;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-09 @ 3:13 a.m..
 */
public class XapiParserPlugin implements Plugin<Project> {

    private boolean strict = true;

    @Override
    public void apply(Project proj) {
        if ("true".equals(proj.findProperty("xapiModern"))) {
            throw new IllegalStateException("Do not apply xapi-parser plugin to modern module " + proj.getPath());
        }
        ProjectDescriptorView rootView = ProjectDescriptorView.rootView(proj.getGradle());
        final Project rootProj = proj.getRootProject();
        proj.getLogger().info("Setting up xapi parser plugin for {}", proj.getPath());
        HasAllProjects map = SchemaMap.fromView(rootView);
        if (proj != rootProj) {
            // we _always_ want to make the very first xapi-parser plugin an invocation on root project.
            // This ensures consistent timing w.r.t. when build graph callbacks are invoked.
            final PluginContainer rootPlugins = proj.getRootProject().getPlugins();
            rootPlugins.apply("base");
            rootPlugins.apply("xapi-parser");

        } else {
            // This is always the first block of code to get out of this if/else combination.
            // any non-root project applies to root, and then we get here, to the only safe "run once per build" code block in this class.

            // forcibly finish the parsing of the schema, if it's not already done.
            // we pay a little latency up front so all the rest of our code can assume the entire schema is parsed.
            // we _should_ still be able to defer indexing, which costs considerably more as it is many file writes
            // instead of a few file reads. We resolve() now, to avoid both timing and locking issues:
            // it's easier to reason about the schema if we don't need to pass 50x callbacks.
            map.resolve();
            // For users using xapi-loader in their settings.gradle, the map will already be resolved by now.
        }

        // always eagerly initialize build graph
        BuildGraph buildGraph = BuildGraph.findBuildGraph(proj);
        final ProjectGraph projectGraph = buildGraph.getProject(proj.getPath());
        projectGraph.drainTasks(ReadyState.BEFORE_CREATED);

        Object strictProp = proj.findProperty("xapi.strict");
        switch (String.valueOf(strictProp)) {
            case "false":
            case "0":
            case "n":
            case "no":
            case "False":
                strict=false;
        }
        proj.getPlugins().apply(XapiBasePlugin.class);
        final ProjectView view = ProjectView.fromProject(proj.getRootProject());
        boolean madeChanges = false;

        String gradleProjectVersion = String.valueOf(proj.getVersion());
        final File schemaFile = map.getRootSchemaFile();
        if ("unspecified".equals(gradleProjectVersion)) {
            madeChanges = true;
            proj.setVersion(map.getVersion());
        } else if (QualifiedModule.UNKNOWN_VALUE.equals(map.getVersion())) {
            map.setVersion(gradleProjectVersion);
        } else if (!gradleProjectVersion.equals(map.getVersion())) {
            File settingsFile = proj.getGradle().getStartParameter().getSettingsFile();
            if (settingsFile == null) {
                settingsFile = proj.getGradle().getStartParameter().getProjectDir();
                if (settingsFile == null) {
                    settingsFile = proj.getRootDir();
                }
            }
            if (!settingsFile.isFile()) {
                File parent = settingsFile.isDirectory() ? settingsFile : settingsFile.getAbsoluteFile().getParentFile();
                File child = new File(parent, "settings.gradle.kts");
                if (!child.isFile()) {
                    child = new File(parent, "settings.gradle"); // we want the default printed value to be settings.gradle
                }
                settingsFile = child;
            }
            throw new IllegalStateException("Gradle version and schema.xapi disagree about the version of the project"
             + proj.getPath() + "; gradle:'" + gradleProjectVersion + "', xapi:'" + map.getVersion() + "'\n" +
            "Please make the following files agree about versions:\n" +
            "file://" + settingsFile.getAbsolutePath() + "\n(rootProject.name = \"CorrectValue\")\n" +
            "file://" + schemaFile + "\n(<xapi-schema version = \"CorrectValue\")"
            ); // end new IllegalStateException
        }

        String gradleProjectGroup = String.valueOf(proj.getGroup());
        if (gradleProjectGroup.isEmpty()) {
            madeChanges = true;
            proj.setGroup(map.getGroup());
        } else if (QualifiedModule.UNKNOWN_VALUE.equals(map.getGroup()) || ".".equals(map.getGroup())) {
            map.setGroup(gradleProjectGroup);
        } else if (!gradleProjectGroup.equals(map.getGroup())) {
            throw new IllegalStateException("Gradle group and schema.xapi disagree about the group of the project "
             + proj.getPath() + ";\ngradle:'" + gradleProjectGroup + "', xapi:'" + map.getGroup() + "'\n" +
            "see file://" + proj.getRootDir() + " for details.");
        }

        if (madeChanges) {
            // only bother the user if we did anything.
            proj.getLogger().quiet("Configured '{}:{}:{}' as '{}:{}:{}'.", gradleProjectGroup, proj.getName(), gradleProjectVersion, proj.getGroup(), proj.getName(), proj.getVersion());
        }

        boolean[] foundMe = {false};


        projectGraph.whenReady(ReadyState.BEFORE_CREATED, i->{
            map.flushWork();
        });

        projectGraph.whenReady(ReadyState.CREATED - 0x20, i->{
            map.getCallbacks().forProject(proj.getPath(), schemaProject -> {
                foundMe[0] = true;
                if (!"true".equals(proj.findProperty("xapiModern"))) {
                    proj.getLogger().quiet("Initializing parser plugin for detected project " + proj.getPath());
                    initializeProject(proj, map, schemaProject);
                }
            });
            map.flushWork();
        });

        projectGraph.whenReady(ReadyState.EXECUTE - 1, i->{
            map.flushWork();
        });

        proj.getGradle().buildFinished(result -> {
            if (!foundMe[0]) {
                proj.getLogger().info("No schema project entry found for {} in {}; known projects: {}",
                    proj.getPath(),
                    schemaFile.exists() ? "file://" + schemaFile : "<virtual schema>",
                    map.getAllProjects().map(SchemaProject::getPathGradle).join(", ")
                );
            }
        });

        // we're migrating, so legacy code slowly being killed off...
//        if (":".equals(proj.getPath())) {
//            loadFromIndex(view, map, projectGraph);
//        }
    }

    /**
     * Initialize the given project graph based on pre-computed on-disk xapi index.
     *
     * This allows us to pre-populate all exposed dependency Configurations very early
     * during the configuration evaluation phase of the gradle build.
     *
     * Whenever anybody adds the xapi-parser plugin, we force root project to have xapi-parser,
     * and then when we get to root project loadFromIndex, we pre-create all "live"
     * configurations for all gradle projects.
     *  @param rootView The ProjectView that we will use to get access to a ProjectView for all relevant projects.
     * @param map
     * @param graph The graph of the project to initialize. When it is the root project,
     */
    private void loadFromIndex(final ProjectView rootView, final HasAllProjects map, final ProjectGraph graph) {
        final SchemaIndex index = map.getIndexProvider().out1();
        SchemaIndexReader reader = index.getReader();

        for (SchemaProject proj : map.getAllProjects()) {
            ProjectView view = rootView.findProject(proj.getPathGradle());
            if (view == null) {
                throw new IllegalStateException("No view found for " + proj.getPathGradle());
            }
            final ProjectGraph viewPg = view.getProjectGraph();
            viewPg.whenReady(ReadyState.AFTER_CREATED, r->{
                for (SchemaPlatform platform : proj.getAllPlatforms()) {
                    for (SchemaModule module : proj.getAllModules()) {
                        if (reader.hasEntries(view, proj.getPathIndex(), platform, module)) {
                            // create incoming and outgoing configurations.
                            final String configRoot = PlatformModule.unparse(platform.getName(), module.getName());
                            String configTransitive = toConfigName(configRoot, "Transitive");
                            String configIntransitive = toConfigName(configRoot, "Intransitive");
                            String configOut = toConfigName(configRoot, "Out");
                            String configExportCompile = toConfigName(configRoot, "ExportCompile");
                            String configExportRuntime = toConfigName(configRoot, "ExportRuntime");


                            final Logger log = view.getLogger();
                            if (log != null) {
                                log.trace("{}, creating i/o configurations {}, {} and {}", proj.getPathGradle(), configTransitive, configIntransitive, configOut);
                            }

                            final Configuration transitive = view.getConfigurations().maybeCreate(configTransitive);
                            //                    transitive.setCanBeResolved(false);
                            //                    transitive.setCanBeConsumed(false);
                            final Configuration intransitive = view.getConfigurations().maybeCreate(configIntransitive);
                            //                    intransitive.setCanBeResolved(false);
                            //                    intransitive.setCanBeConsumed(false);

                            final Configuration out = view.getConfigurations().maybeCreate(configOut);
                            //                    out.setCanBeResolved(false);
                            //                    // the "out" configuration is for anyone
                            //                    out.setCanBeConsumed(true);

                            final SourceSet srcSet = view.getSourceSets().maybeCreate(configRoot);
//                            final Configuration exportCompile = view.getConfigurations().maybeCreate(configExportCompile);
//                            final Configuration exportRuntime = view.getConfigurations().maybeCreate(configExportRuntime);
//                            final JavaCompile javac = view.getTasks().maybeCreate(srcSet.getCompileJavaTaskName(), JavaCompile.class);
//                            javac.getSource().plus(srcSet.getAllJava());
//                            javac.getClasspath().plus(srcSet.getCompileClasspath());
//                            JavaPluginsHelper.registerClassesDirVariant(view.getTasks().named(srcSet.getCompileJavaTaskName(), JavaCompile.class), view.getObjects(), exportRuntime);
//                            exportRuntime.setCanBeConsumed(true);
                            //                    final Provider<? extends AbstractCompile> compile;
                            //                    JavaPluginsHelper.registerClassesDirVariant(compile, view.getObjects(), out);

                        } else {
                            // hm, now that we know the module is not realized, we should mark it as such... (.prune()?)
                        }

                    }

                }
            });
        }
    }

    private void initializeProject(
        Project gradleProject,
        HasAllProjects map,
        SchemaProject schemaProject
    ) {
        final ProjectView view = ProjectView.fromProject(gradleProject);
        final MappedIterable<? extends SchemaPlatform> platforms = schemaProject.getAllPlatforms()
                .counted();
        final MappedIterable<? extends SchemaModule> modules = schemaProject.getAllModules()
                .counted();

        final XapiSchema schema = view.getSchema();

        for (SchemaPlatform schemaPlatform : platforms) {
            schema.getPlatforms().maybeCreate(schemaPlatform.getName());
        }
        for (SchemaPlatform schemaPlatform : platforms) {
            Lazy<PlatformConfigInternal> platProvider = Lazy.deferred1(()->{
                final PlatformConfigInternal platform = schema.getPlatforms().maybeCreate(schemaPlatform.getName());
                if (schemaPlatform.getReplace() != null) {
                    platform.replace(schemaPlatform.getReplace());
                }
                platform.setPublished(schemaPlatform.isPublished());
                platform.setTest(schemaPlatform.isTest());
                return platform;
            });

            for (SchemaModule schemaModule : modules) {
                final Lazy<SchemaIndex> indexProvider = map.getIndexProvider();
                final SchemaIndex index = indexProvider.out1();
                final SchemaIndexReader reader = index.getReader();

                if (reader.hasEntries(view, schemaProject.getPathIndex(), schemaPlatform, schemaModule)) {
                    final PlatformConfigInternal platform = platProvider.out1();
                    final ArchiveConfigInternal archive = platform.getArchives().maybeCreate(schemaModule.getName());
                    archive.setPublished(schemaPlatform.isPublished() && schemaModule.isPublished());
                    archive.setTest(schemaPlatform.isTest() || schemaModule.isTest());
                    for (String include : schemaModule.getInclude()) {
    //                    archive.require(isMainPlatform ? require : platform.getName() + GUtil.toCamelCase(require));
                        // we can use above commented line when we remove hideous "fixRequires" hack from xapi production build
                        archive.require(include);
                    }
                }
            }
        }

        for (PlatformConfig platform : schema.getPlatforms()) {
            for (ArchiveConfig module : platform.getArchives()) {

                final Lazy<SchemaIndex> indexProvider = map.getIndexProvider();
                final SchemaIndex index = indexProvider.out1();
                final SchemaIndexReader reader = index.getReader();
                final SchemaPlatform plat = schemaProject.getPlatform(platform.getName());
                final SchemaModule mod = schemaProject.getModule(module.getName());

                if (reader.hasEntries(view, schemaProject.getPathIndex(), plat, mod)) {
                    final PlatformModule platMod = new PlatformModule(plat.getName(), mod.getName());
                    for (SchemaDependency dep : schemaProject.getDependenciesOf(plat, mod)) {
                        dep = dep.rebase(platMod);
                        addDependency(view, module, map, schemaProject, dep);
                    }
                }

            }
        }

    }

    private void addDependency(
        ProjectView view,
        ArchiveConfig consumerConfig,
        HasAllProjects map,
        SchemaProject schemaProject,
        SchemaDependency dep
    ) {
        String consumerPlatform = consumerConfig.getPlatform().getName();
        String consumerModule = consumerConfig.getName();
        // dirty... we _probably_ shouldn't be resolving these so eagerly....
        final ArchiveGraph owner = view.getProjectGraph().platform(
            consumerPlatform).archive(consumerModule);
        switch(dep.getType()) {
            case unknown:
                // for unknown types, we should probably log a warning, or try for multiple sources...

            case project:
                // project: this dependency is an intra-build project reference.
                String name = dep.getName();
                final Maybe<SchemaProject> result = map.findProject(name);
                if (result.isPresent()) {
                    SchemaProject toRequire = result.get();
                    PlatformModule requiredPlatform = dep.getCoords();
                    view.getLogger().quiet("Project {} platform {} adding {}", result.get(), requiredPlatform, consumerConfig.getPath());
                    if (requiredPlatform.getPlatform() == null) {
                        // all platforms
                        if (requiredPlatform.getModule() == null) {
                            // all modules
                            schemaProject.forAllPlatforms(platform-> {
                                // add dependency from all platform/module combinations
                                schemaProject.forAllModules(module-> {
                                    // this may be cutting to the chase too quickly;
                                    // we should instead probably be recording complete dependency metadata
                                    // (and doing so when processing settings, not projects, like we are doing here)
                                    owner.importProject(name, platform.getName(), module.getName(), Transitivity.api, false);

                                });
                            });
                        } else {
                            // just one module, but do all compatible platforms
                            map.getCallbacks().forModule(requiredPlatform.getModule(), requiredMod-> {
                                    schemaProject.forAllPlatforms(platform -> {
                                        // setup a dependency for platform:requiredMod
                                        final SchemaPlatform childPlatform = toRequire.findPlatform(platform.getName());
                                        if (childPlatform == null) {
                                            // TODO: if there are parent platforms, we _may_ want to add them...
                                            //  though, they should be transitive based on intra-module dependencies already.
                                        } else {
                                            // this should actually, most likely be something that just records metadata of the dependency...
                                            // i.e. a complete graph where each node identifies a project:platform:module,
                                            // with all other coordinates that node requires and is required by.
                                            owner.importProject(name, platform.getName(), requiredMod.getName(), Transitivity.api, false);
                                        }
                                    });
                            });
                        }
                    } else {
                        // There is a platform requested
                        if (requiredPlatform.getModule() == null) {
                            // all modules
                            schemaProject.forAllModules(module->{
                                owner.importProject(name, requiredPlatform.getPlatform(), module.getName(), Transitivity.api, false);
                            });
                        } else {
                            // just one module, just one platform
                            owner.importProject(name, requiredPlatform.getPlatform(), requiredPlatform.getModule(), Transitivity.api, false);
                        }

                    }
                } else {
                    view.getLogger().quiet("Could not find project ", name, " in ", map);
                }
                break;
            case internal:
                // internal: this dependency is intra-project, like gwt.main -> main.api.source

            case external:
                // external: this dependency is a "fully:qualified:dependency:string"
                // here is where we'll need to depend on a at-settings-time index of the world to be pre-built...

        }
        consumerConfig.request(dep.withCoords(consumerPlatform, consumerModule), ArchiveRequestType.COMPILE);
    }
}

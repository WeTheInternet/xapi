package net.wti.loader.plugin;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.loader.impl.BuildScriptBuffer;
import net.wti.gradle.loader.impl.ClosureBuffer;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.*;
import net.wti.gradle.schema.index.SchemaIndexerImpl;
import net.wti.gradle.schema.map.SchemaMap;
import net.wti.gradle.schema.parser.DefaultSchemaMetadata;
import net.wti.gradle.schema.parser.SchemaParser;
import net.wti.gradle.schema.spi.SchemaIndexer;
import net.wti.gradle.schema.spi.SchemaProperties;
import net.wti.gradle.settings.ProjectDescriptorView;
import net.wti.gradle.settings.RootProjectView;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.GFileUtils;
import xapi.constants.X_Namespace;
import xapi.dev.source.LocalVariable;
import xapi.fu.*;
import xapi.fu.data.ListLike;
import xapi.fu.java.X_Jdk;
import xapi.string.X_String;

import javax.inject.Inject;
import java.io.File;

import static net.wti.gradle.settings.ProjectDescriptorView.rootView;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 1:33 AM.
 */
public class XapiLoaderPlugin implements Plugin<Settings> {

    private final Logger logger;

    @Inject
    public XapiLoaderPlugin() {
        logger = Logging.getLogger(XapiLoaderPlugin.class);
    }

    @Override
    public void apply(Settings settings) {
        // Read in the root schema.xapi file
        final File schema = new File(settings.getRootDir(), "schema.xapi");
        final RootProjectView root = rootView(settings);
        if (schema.exists()) {

            final SchemaParser parser = ()->root;
            final DefaultSchemaMetadata metadata = parser.getSchema();
            if ("".equals(settings.getRootProject().getName())) {
                logger.quiet("Configuring default root project name of file://{} to {}", settings.getRootDir(), metadata.getName());
                settings.getRootProject().setName(metadata.getName());
            }

            // Remove anything disabled by system property / env var
            String explicitPlatform = getPlatform(settings);
            if (explicitPlatform != null) {
                metadata.reducePlatformTo(explicitPlatform);
            }


            // Write the index...
            String propertiesClass = MinimalProjectView.searchProperty(X_Namespace.PROPERTY_SCHEMA_PROPERTIES_INJECT, root);
            SchemaProperties properties;
            if (X_String.isNotEmpty(propertiesClass)) {
                final Class<?> cls;
                try {
                    cls = Thread.currentThread().getContextClassLoader().loadClass(propertiesClass);
                    if (!SchemaProperties.class.isAssignableFrom(cls)) {
                        throw new IllegalArgumentException("Class " + cls + " is not a (recognizable) instance of " + SchemaProperties.class);
                    }
                    properties = (SchemaProperties) cls.newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException("Unable to find class " + propertiesClass +"; set by property " + X_Namespace.PROPERTY_SCHEMA_PROPERTIES_INJECT, e);
                }
            } else {
                properties = SchemaProperties.getInstance();
            }

            // Transverse the full */*/schema.xapi hierarchy
            final SchemaMap map = buildMap(settings, parser, metadata, properties);

            root.whenReady(view -> {
                // resolve the block-until-index-done task
                root.settingsReady();
                final SchemaIndex result = map.getIndexProvider().out1();
                // Setup callback for each project to add buildable / publishable component with multiple modules / platforms.
                prepareProjects(view, result, properties, settings, map);
                // Flush out callbacks in the priorities they were declared
                map.getCallbacks().flushCallbacks(map);
            });

        } else {
            // no root schema.xapi... consider instead a reverse-lookup for schema.xapi in any include()d projects?
            // if we do that, we should do it universally, and just make sure to avoid double-parse in the case of
            // "there was a root schema that we loaded some potentially virtual projects from"
        }
    }

    protected SchemaMap buildMap(
        Settings settings,
        SchemaParser parser,
        DefaultSchemaMetadata metadata,
        SchemaProperties properties
    ) {
        //noinspection UnnecessaryLocalVariable (nice for debugging)
        SchemaMap map = SchemaMap.fromView(parser.getView(), parser, metadata, properties);
        return map;
    }

    private void prepareProjects(final MinimalProjectView view, final SchemaIndex fullIndex, final SchemaProperties properties, Settings settings, SchemaMap map) {
        SchemaIndexReader index = fullIndex.getReader();

        // In order to access Project objects from within code running while settings.gradle is being processed,
        // we'll just setup beforeProject/afterProject listeners:

        settings.getGradle().beforeProject(p->{
            final Maybe<SchemaProject> match = map.getAllProjects()
                    .firstMatch(m -> m.getPathGradle().equals(p.getPath()));
            if (match.isPresent()) {
                // This is a gradle project who matches one of our schema-backed project declarations.
                // Lets setup some support utilities

                setupProject(p, match.get(), properties, index, map);
            }
        });
        settings.getGradle().afterProject(p->{
            final Maybe<SchemaProject> match = map.getAllProjects()
                    .firstMatch(m -> m.getPathGradle().equals(p.getPath()));
            if (match.isPresent()) {
                // This is a gradle project who matches one of our schema-backed project declarations.
                // Lets setup some support utilities

                finalizeProject(p, match.get(), properties, index, map);
            }
        });

        map.getCallbacks().perProject(project -> {
            logger.info("Processing xapi schema for project {}", project);
            String gradlePath = ":" + project.getSubPath().replace('/', ':');
            if (project != map.getRootProject()) {
                File dir = new File(settings.getSettingsDir(), project.getSubPath());
                settings.include(gradlePath);
                if (dir.isDirectory()) {
                    final ProjectDescriptor p = settings.project(gradlePath);
                    if (new File(dir, project.getName() + ".gradle").exists()) {
                        p.setProjectDir(dir);
                        p.setBuildFileName(project.getName() + ".gradle");
                    } else if (new File(dir, project.getName() + ".gradle.kts").exists()) {
                        p.setProjectDir(dir);
                        p.setBuildFileName(project.getName() + ".gradle.kts");
                    } else {
                        // TODO generate a useful "defaults" script that can be applied / used as default buildscript
                    }
                }
            }
            // create sub-projects; one for each platform/module pair.
            final int[] liveCnt = {0};
            project.forAllPlatformsAndModules((plat, mod) -> {
                String key = QualifiedModule.unparse(plat.getName(), mod.getName());
                String modKey = project.getName() + "-" + key;
                String projectName = gradlePath + (gradlePath.endsWith(":") ? "" : ":") + key;
                String projectSrcPath = "src/gradle/" + key;
                String projectOutputPath = "mods/" + key;
                map.whenResolved(() -> {
                    boolean isLive = index.hasEntries(view, project.getPathIndex(), plat, mod);
                    if (isLive) {
                        if (++liveCnt[0] > 1) {
                            if (!project.isMultiplatform()) {
                                throw new IllegalStateException("A project (" + project.getPathGradle() + ") with more than one live module must be multiplatform=true (cannot be standalone)");
                            }
                        }
                        // to be able to avoid creating gradle projects we don't _need_,
                        // we'll check on the written index to decide whether to create said projects or not.
                        // user may freely create and check in their own projects, and we'll happily hook them up,
                        // so, by default, only schema.xapi / indexed dependencies can trigger a generated project creation.
                        final File projectRoot = project.getView().getProjectDir();
                        String buildFileName = project.getName() + GradleCoerce.toTitleCase(key) + ".gradle";
                        final File projDir = new File(projectRoot, projectOutputPath);
                        if (new File(projDir, buildFileName + ".kts").exists()) {
                            buildFileName = buildFileName + ".kts";
                        }
                        final File buildFile = new File(projDir, buildFileName);
                        if (project.isMultiplatform()) {
                            view.getLogger().info("Multiplatform {} : {} file://{} @ {}", project.getPathGradle(), modKey, buildFile, key);
                            settings.include(projectName);
                            final ProjectDescriptor proj = settings.findProject(projectName);
                            proj.setProjectDir(projDir);
                            proj.setBuildFileName(buildFileName);
                            proj.setName(modKey);
                        } else {
                            // intentionally not using Monoplatform; it blends into Multiplatform too easily in logs
                            view.getLogger().info("Singleplatform {} : {} file://{} @ {}", project.getPathGradle(), modKey, buildFile, key);
                        }

                        File projectSource = new File(projectRoot, projectSrcPath);

                        // TODO: add a README.md into projectSource, describing how to contribute code to src/gradle/platMod/<magic filenames>

                        BuildScriptBuffer out = new BuildScriptBuffer();
                        final In2<String, Out1<Printable<?>>> maybeAdd =
                                (name, buffer) -> {
                                    In2<String, File> cb = (imports, file) ->
                                            buffer.out1()
                                                    .print("// GenStart ").print(name).print(" from file://").println(file.getAbsolutePath())
                                                    .println(imports)
                                                    .print("// GenEnd ").print(name).print(" from file://").println(file.getAbsolutePath());
                                    maybeRead(projectSource, name, cb);
                                };
                        final Out1<Printable<?>> getBody = Immutable.immutable1(out);
                        if (projectSource.exists()) {
                            // Use src/gradle/key path to look for files to use to assemble a build script for us.
                            // since we don't need any such thing yet, just leaving a space here for it to be done later.

                            maybeRead(projectSource, "imports", (imports, file) -> {
                                for (String s : imports.split("[\\n\\r]+")) {
                                    out.addImport(s);
                                }
                            });
                            final Out1<Printable<?>> getBuildscript = out::getBuildscript;
                            maybeAdd.in("buildscript.start", getBuildscript);
                            maybeAdd.in("buildscript", getBuildscript);
                            maybeAdd.in("buildscript.end", getBuildscript);

                            final In2<String, File> addPlugin = (imports, file) -> {
                                for (String s : imports.split("[\\n\\r]+")) {
                                    if (!s.startsWith("//")) {
                                        out.addPlugin(s);
                                    }
                                }
                            };
                            maybeRead(projectSource, "plugins.start", addPlugin);
                            maybeRead(projectSource, "plugins", addPlugin);
                            maybeRead(projectSource, "plugins.end", addPlugin);

                            maybeAdd.in("body.start", getBody);
                            maybeAdd.in("body", getBody);
                        }


                        out.println("String javaPlugin = findProperty('xapi.java.plugin') ?: 'java-library'");
                        out.println("apply plugin: javaPlugin");
                        // To start, setup the sourceset!
                        String srcSetName = project.isMultiplatform() ? "main" : key;
                        LocalVariable main = out.addVariable(SourceSet.class, srcSetName, true);
                        main.setInitializerPattern("sourceSets.maybeCreate('$1')", srcSetName);
                        main.invoke("java.setSrcDirs($2)", "[]");
                        main.invoke("resources.setSrcDirs($2)", "[]");

                        // lets have a look at the types of source dirs to decide what sourcesets to create.
                        File moduleSource = new File(projectRoot, "src" + File.separator + key);
                        if (moduleSource.isDirectory()) {
                            for (String sourceDir : moduleSource.list()) {
                                switch (sourceDir) {
                                    case "java":
                                    case "groovy":
                                    case "kotlin":
                                        main.access("java.srcDir(\"$2/$3\")",
                                                moduleSource.getAbsolutePath().replace(projectRoot.getAbsolutePath(), "$projectDir/../.."),
                                                sourceDir);
                                        break;
                                    case "resources":
                                        main.access("resources.srcDir(\"$2/resources\")", moduleSource.getAbsolutePath());
                                        break;
                                    default:
                                        // perhaps warn about unused source directory? ...not really worth it.
                                        // check for java-versioned directories...
                                        if (sourceDir.startsWith("javaGT")) {
                                            // applies to java versions greater than $N
                                            // TODO: option to autogen module.java for javaGT8
                                        } else if (sourceDir.startsWith("javaLT")) {
                                            // applies to java versions less than $N
                                        } else if (sourceDir.startsWith("java")) {
                                            // applies to java version equals $N
                                        }
                                }
                            }
                        }
                        Lazy<ClosureBuffer> dependencies = Lazy.deferred1(()->
                                out.startClosure("dependencies")
                        );
                        for (SchemaDependency dependency : project.getDependenciesOf(plat, mod)) {
                            if (!index.dependencyExists(dependency, project, plat, mod)) {
                                // skipping a non-live dependency.
                                continue;
                            }
                            final ClosureBuffer depOut = dependencies.out1();
                            switch (dependency.getTransitivity()) {
                                case api:
                                    out.addPlugin("java-library");
                                    depOut.append(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "api" : key + "Api"
                                    );
                                    break;
                                case compile_only:
                                    depOut.append("compileOnly");
                                    depOut.append(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "compileOnly" : key + "CompileOnly"
                                    );
                                    break;
                                case impl:
                                    depOut.append(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "implementation" : key + "Implementation"
                                    );
                                    break;
                                case runtime:
                                    depOut.append(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "runtime" : key + "Runtime"
                                    );
                                    break;
                                case runtime_only:
                                    depOut.append(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "runtimeOnly" : key + "RuntimeOnly"
                                    );
                                    break;
                                default:
                                    throw new IllegalArgumentException("transitivity " + dependency.getTransitivity() + " is not supported!");
                            }
                            depOut.append(" ");
                            switch (dependency.getType()) {
                                case unknown:
                                case project:
                                    String path = dependency.getName();
                                    if (!path.startsWith(":")) {
                                        path = ":" + path;
                                    }
                                    PlatformModule coords = dependency.getCoords();
                                    if (coords.getPlatform() == null) {
                                        coords = coords.edit(plat.getName(), null);
                                    }
                                    if (coords.getModule() == null) {
                                        coords = coords.edit(null, mod.getName());
                                    }
                                    // compute whether the target project is not-multiplatform / needs configuration: set too.
                                    final String unparsed = QualifiedModule.unparse(coords);
                                    if (index.isMultiPlatform(view, path, coords)) {
                                        // multi-platform needs to convert to a subproject dependency.
                                        String simpleName = path.substring(path.lastIndexOf(":") + 1);
                                        path = path + ":" + simpleName + "-" + unparsed;
                                    } else {
                                        // not multi-platform, we expect this to be a single-module dependency,
                                        // so, for now, no need to do anything; a plain project dependency is correct.
//                                    path = path + "\", configuration: \"" + unparsed + "Out";
                                    }
                                    depOut.println("project(path: \"" + path + "\")");
                                    break;
                                case internal:
                                    // to have an internal dependency is to inherently be multiplatform
                                    // (there is no distinction between platform and module when it comes to "do you have more than one module")
                                    path = project.getPathGradle();
                                    if (":".equals(path)) {
                                        path = project.getName();
                                    }
                                    String simpleName = path.substring(path.lastIndexOf(":") + 1);
                                    PlatformModule platMod = PlatformModule.parse(dependency.getName());
                                    if (index.isMultiPlatform(view, project.getPathIndex(), platMod)) {
                                        path = gradlePath + (gradlePath.endsWith(":") ? "" : ":") + simpleName + "-" + platMod.toPlatMod();
                                    }
                                    depOut.println("project(path: \"" + path + "\")");
                                    break;
                                case external:
                                    depOut.println("\"" + dependency.getGNV() + "\"");
                                    continue;
                            } // end switch(dep.getType())
                        } // end for (dep :

                        if (!project.isMultiplatform()) {
                            out.println()
                                    .startClosure("configurations")
                                    .println(key + "Out");
                            // TODO: actually hook this up in the generated code
                        }

                        if (projectSource.exists()) {
                            maybeAdd.in("body.end", getBody);
                        }

                        // all done writing generated project
                        GFileUtils.writeFile(out.toSource(), buildFile, "UTF-8");
                    }
                });
            });
        });
    }

    private void finalizeProject(final Project gradleProject, final SchemaProject project, final SchemaProperties properties, final SchemaIndexReader index, final SchemaMap map) {
        if (!project.isMultiplatform()) {
//            gradleProject.getLogger().quiet("Finalizing {}. Configurations: {}", gradleProject.getPath(), gradleProject.getConfigurations().getNames());
            gradleProject.getGradle().projectsEvaluated(g->{
                gradleProject.apply(o-> {
                    gradleProject.getLogger().quiet("Finalizing {}. Configurations: {}", gradleProject.getPath(), gradleProject.getConfigurations().getNames());
//                    o.plugin(XapiParserPlugin.class);
                });
            });
        }
    }

    private void setupProject(final Project gradleProject, final SchemaProject project, final SchemaProperties properties, final SchemaIndexReader index, final SchemaMap map) {
        if (!project.isMultiplatform()) {
            gradleProject.apply(o-> {
                o.plugin(JavaLibraryPlugin.class);
//                o.plugin(XapiJavaPlugin.class);
            });
        }
        final ProjectView view = ProjectView.fromProject(gradleProject);
        view.getLogger().quiet("Setting up schema for project {}", view.getPath());
        ListLike<SchemaDependency> externals = X_Jdk.list();
//        project.getDependencies().forEachPair((mod, dep) -> {
//            switch (dep.getType()) {
//                case external:
//                    externals.add(dep);
//                    break;
//            }
//        });
        if (externals.isNotEmpty()) {
//            view.getTasks().register("resolveExternals", ResolveExternalsTask.class);
//            TaskSpy.spy(view, "resolveExternals", ResolveExternalsTask.class, resolve->{
//                // make all classpaths / configuration resolution depend on resolveExternals.
//            });
        }
        if (project.isMultiplatform()) {
            // If we are multi-platform, then we have sub-projects for each of our modules.
            // These subprojects will not match the SchemaProject, nor will they need to:
            // these subprojects contain generated dependency / general wiring;
            // any API-sugar we sprinkle on top goes into the multiplatform aggregator (the parent of all modules)

        } else {

            // A non-multi-platform project needs to have a bit more "hidden surgery".
            project.forAllPlatformsAndModules((plat, mod) -> {
                if (index.hasEntries(view, project.getPathIndex(), plat, mod)) {
                    // for now, lets apply our generated gradle source...
                    // if this gets too yucky, we can just replace it w/ direct (but hidden) "act on Project" object code
                    String key = QualifiedModule.unparse(plat.getName(), mod.getName());
                    String path = view.getProjectDir().getAbsolutePath() +
                            File.separator + "mods" + File.separator + key + File.separator +
                            project.getName() + X_String.toTitleCase(key) + ".gradle";
                    if (new File(path + ".kts").exists()) {
                        path = path + ".kts";
                    }
                    final String finalPath = path;
                    view.getLogger().quiet("{} is sourcing generated path {}", view.getPath(), finalPath);
                    gradleProject.apply(o-> {
                        o.from(finalPath);
                    });
                }
            });
        }
    }

    private void maybeRead(final File dir, final String file, final In2<String, File> callback) {
        File target = new File(dir, file);
        if (target.isFile()) {
            final String contents = GFileUtils.readFile(target);
            callback.in(contents, target);
        }
    }

    private static String getPlatform(Settings settings) {
        String explicitPlatform = settings.getStartParameter().getProjectProperties().get("xapi.platform");
        if (explicitPlatform == null) {
            explicitPlatform = settings.getStartParameter().getSystemPropertiesArgs().get("xapi.platform");
        }
        if (explicitPlatform == null) {
            explicitPlatform = System.getProperty("xapi.platform");
        }
        if (explicitPlatform == null) {
            explicitPlatform = System.getenv("XAPI_PLATFORM");
        }
        return explicitPlatform;
    }
}

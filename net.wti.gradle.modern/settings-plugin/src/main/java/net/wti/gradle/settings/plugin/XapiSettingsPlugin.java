package net.wti.gradle.settings.plugin;

import net.wti.gradle.api.BuildCoordinates;
import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.settings.XapiSchemaParser;
import net.wti.gradle.settings.api.*;
import net.wti.gradle.settings.index.SchemaIndex;
import net.wti.gradle.settings.schema.DefaultSchemaMetadata;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Action;
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
import xapi.dev.source.BuildScriptBuffer;
import xapi.dev.source.ClosureBuffer;
import xapi.dev.source.LocalVariable;
import xapi.fu.*;
import xapi.fu.data.ListLike;
import xapi.fu.java.X_Jdk;
import xapi.string.X_String;

import javax.inject.Inject;
import java.io.File;

/**
 * XapiSettingsPlugin:
 * <p>
 * <p>Created to supercede the entirely gnarly mess of the previous xapi gradle plugins,
 * <p>which got stuck on a custom build of gradle for stupid reasons, that are too hard to change.
 * <p>
 * <p>Thus, we'll be creating a successor to all other xapi gradle plugins, xapi-settings,
 * <p>which takes all the lessons of all previous iterations, resulting in a simple, efficient, nice-to-use replacement.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 03/06/2024 @ 12:31 a.m.
 */
public class XapiSettingsPlugin implements Plugin<Settings> {

    private final Logger logger;

    @Inject
    public XapiSettingsPlugin() {
        logger = Logging.getLogger(XapiSettingsPlugin.class);
    }
    @Override
    public void apply(final Settings settings) {
        final File schema = new File(settings.getRootDir(), "schema.xapi");
        RootProjectView root = RootProjectView.rootView(settings);
        if (schema.exists()) {
            final XapiSchemaParser parser = ()->root;
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
                        throw new IllegalArgumentException("Class " + cls + " is not a (recognizable) instance of " + SchemaProperties.class
                                + ". You may need to run ./gradlew --stop");
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

            final Action<? super MinimalProjectView> callback = ready -> {
                // resolve the block-until-index-done task
                root.settingsReady();
                final SchemaIndex result = map.getIndexProvider().out1();
                // Setup callback for each project to add buildable / publishable component with multiple modules / platforms.
                prepareProjects(root, result, properties, settings, map);
                // Flush out callbacks in the priorities they were declared
                map.getCallbacks().flushCallbacks(map);
            };

//            if (null == System.getProperty("idea.version")) {
            // normal gradle build, wait until later to resolve
            settings.getGradle().settingsEvaluated(ready -> {
                callback.execute(root);
            });
        } else {
            logger.warn("No schema.xapi found in " + settings.getRootDir() + "; skipping xapi-settings setup.");
        }
    }


    protected SchemaMap buildMap(
            Settings settings,
            XapiSchemaParser parser,
            DefaultSchemaMetadata metadata,
            SchemaProperties properties
    ) {
        //noinspection UnnecessaryLocalVariable (nice for debugging)
        SchemaMap map = SchemaMap.fromView(parser.getView(), parser, metadata, properties);
        return map;
    }

    private void prepareProjects(final RootProjectView view, final SchemaIndex fullIndex, final SchemaProperties properties, Settings settings, SchemaMap map) {
        SchemaIndexReader index = fullIndex.getReader();

        // In order to access Project objects from within code running while settings.gradle is being processed,
        // we'll just setup beforeProject/afterProject listeners:

        view.getLogger().quiet("All projects ({}): {}", map.getAllProjects().size(), map.getAllProjects());
        settings.getGradle().beforeProject(p->{
            final Maybe<SchemaProject> match = map.getAllProjects()
                    .firstMatch(m -> {
                        return m.getPathGradle().equals(p.getPath());
                    });
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
            } else {
                // warn about a missing project?
            }
        });

        map.getCallbacks().perProject(project -> {
            logger.trace("Processing xapi schema for project {}", project);
            String gradlePath = ":" + project.getSubPath().replace('/', ':');
            if (project != map.getRootProject()) {
                File dir = new File(settings.getSettingsDir(), project.getSubPath());
                logger.info("Including {} for project {}", gradlePath, project);
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
                String projectOutputPath = "src/" + key;
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

                        if (dryRun()) {
                            view.getLogger().quiet("Dry run exiting early; found project {} at index {} with build file {}", project.getPathGradle(), project.getPathIndex(), buildFile.getAbsolutePath());
                            return;
                        }

                        if (project.isMultiplatform()) {
                            view.getLogger().info("Multiplatform {} -> {} file://{} @ {}", projectName, modKey, buildFile, key);
                            settings.include(projectName);
                            final ProjectDescriptor proj = settings.findProject(projectName);
                            proj.setProjectDir(projDir);
                            proj.setBuildFileName(buildFileName);
                            proj.setName(modKey);
                        } else {
                            // intentionally not using Monoplatform; it blends into Multiplatform too easily in logs
                            view.getLogger().info("Singleplatform {} -> {} file://{} @ {}", project.getPathGradle(), modKey, buildFile, key);
                        }

                        File projectSource = new File(projectRoot, projectSrcPath);

                        // TODO: add a README.md into projectSource, describing how to contribute code to src/gradle/platMod/<magic filenames>

                        BuildScriptBuffer out = new BuildScriptBuffer();
                        final In2Out1<String, Out1<Printable<?>>, Boolean> maybeAdd =
                                (name, buffer) -> {
                                    In2<String, File> cb = (script, file) ->
                                            buffer.out1()
                                                    .print("// GenInclude ").print(name).print(" from file://").println(file.getAbsolutePath())
                                                    .println(script)
                                            ;
                                    return maybeRead(projectSource, name, cb);
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
                            maybeAdd.io("buildscript.start", getBuildscript);
                            maybeAdd.io("buildscript", getBuildscript);
                            maybeAdd.io("buildscript.end", getBuildscript);

                            final In2<String, File> addPlugin = (plugins, file) -> {
                                out.getPlugins().printBefore("// GenInclude plugin from file://").println(file.getAbsolutePath());
                                for (String s : plugins.split("[\\n\\r]+")) {
                                    if (!s.startsWith("//")) {
                                        out.addPlugin(s);
                                    }
                                }
                                out.getPlugins().println();
                            };
                            maybeRead(projectSource, "plugins.start", addPlugin);
                            maybeRead(projectSource, "plugins", addPlugin);
                            maybeRead(projectSource, "plugins.end", addPlugin);

                            maybeAdd.io("body.start", getBody);
                            maybeAdd.io("body", getBody);
                        }

                        out.println("// GenStart " + getClass().getSimpleName());

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
                                                "$projectDir",
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
                                    if (out.addPlugin("java-library")) {
                                        out.getPlugins().printBefore("// GenInclude ")
                                                .print(getClass().getSimpleName())
                                                .println(" adding java-library b/c api dependencies used");
                                    }
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
                        out.println("// GenEnd " + getClass().getSimpleName());

                        maybeAdd.io("body.end", getBody);
                        out.print("// Done generating buildfile for ")
                                .print(project.getPathGradle())
                                .print(" at file://")
                                .println(buildFile);
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
            // a multi-platform project will make the parent module an aggregator for client modules.
            // single-platform project will leave the main build.gradle file as the "main" module, and uses java-library plugin.
            gradleProject.apply(o-> {
                o.plugin(JavaLibraryPlugin.class);
            });
        }
//        final ProjectView view = ProjectView.fromProject(gradleProject);
//        view.getLogger().quiet("Setting up schema for project {}", view.getPath());

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
            gradleProject.getLogger().quiet("Found externals {} for project {} with index path {}", externals, project.getPathGradle(), project.getPathIndex());
        }
        if (project.isMultiplatform()) {
            // If we are multi-platform, then we have sub-projects for each of our modules.
            // These subprojects will not match the SchemaProject, nor will they need to:
            // these subprojects contain generated dependency / general wiring;
            // any API-sugar we sprinkle on top goes into the multiplatform aggregator (the parent of all modules)
            project.forAllPlatformsAndModules((plat, mod) -> {
                gradleProject.getLogger().quiet("Found multiplatform combination {}-{} for {} with gradle path {}",
                        plat, mod, project.getPathGradle(), gradleProject.getPath());
            });

        } else {
            BuildCoordinates coordinates = RootProjectView.rootView(gradleProject.getGradle());
            // A non-multi-platform project needs to have a bit more "hidden surgery".
            project.forAllPlatformsAndModules((plat, mod) -> {

                if (index.hasEntries(coordinates, project.getPathIndex(), plat, mod)) {
                    // for now, lets apply our generated gradle source...
                    // if this gets too yucky, we can just replace it w/ direct (but hidden) "act on Project" object code
                    if (dryRun()) {
                        gradleProject.getLogger().quiet("Found index entry for {} with index path {}", project.getPathGradle(), project.getPathIndex());
                        return;
                    }
                    String key = QualifiedModule.unparse(plat.getName(), mod.getName());
                    String path = gradleProject.getProjectDir().getAbsolutePath() +
//                            File.separator + "mods" + File.separator + key + File.separator +
                            File.separator + "src" + File.separator + key + File.separator +
                            project.getName() + X_String.toTitleCase(key) + ".gradle";
                    if (new File(path + ".kts").exists()) {
                        path = path + ".kts";
                    }
                    final String finalPath = path;
                    gradleProject.getLogger().quiet("{} is sourcing generated path {}", gradleProject.getPath(), finalPath);
                    gradleProject.apply(o-> {
                        o.from(finalPath);
                    });
                }
            });
        }
    }

    private boolean dryRun() {
        return true;
    }
    private boolean maybeRead(final File dir, final String file, final In2<String, File> callback) {
        File target = new File(dir, file);
        if (target.isFile()) {
            final String contents = GFileUtils.readFile(target);
            callback.in(contents, target);
            return true;
        }
        return false;
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

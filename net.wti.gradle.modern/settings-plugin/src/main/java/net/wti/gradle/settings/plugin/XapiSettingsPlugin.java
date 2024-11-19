package net.wti.gradle.settings.plugin;

import net.wti.gradle.api.BuildCoordinates;
import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.ProjectViewInternal;
import net.wti.gradle.settings.XapiSchemaParser;
import net.wti.gradle.settings.api.*;
import net.wti.gradle.settings.index.IndexNode;
import net.wti.gradle.settings.index.IndexNodePool;
import net.wti.gradle.settings.index.SchemaIndex;
import net.wti.gradle.settings.schema.DefaultSchemaMetadata;
import net.wti.gradle.system.tools.GradleCoerce;
import net.wti.gradle.tools.GradleFiles;
import net.wti.gradle.tools.InternalGradleCache;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.tasks.SourceSet;
import xapi.constants.X_Namespace;
import xapi.dev.source.BuildScriptBuffer;
import xapi.dev.source.ClosureBuffer;
import xapi.dev.source.LocalVariable;
import xapi.fu.*;
import xapi.fu.data.ListLike;
import xapi.fu.data.SetLike;
import xapi.fu.java.X_Jdk;
import xapi.string.X_String;

import javax.inject.Inject;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

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
    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendPattern(X_Namespace.TIMESTAMP_FORMAT)
            .toFormatter();

    @Inject
    public XapiSettingsPlugin() {
        logger = Logging.getLogger(XapiSettingsPlugin.class);
    }
    @Override
    public void apply(final Settings settings) {
        final File schema = new File(settings.getRootDir(), "schema.xapi");
        RootProjectView root = RootProjectView.rootView(settings);
        final IndexNodePool nodes = IndexNodePool.fromSettings(settings);
        if (schema.exists()) {
            String explicitPlatform = getPlatform(settings);
            if (explicitPlatform != null) {
                // add to extensions so code later on can use findProperty() to get platform
                settings.getExtensions().add("xapi.platform", explicitPlatform);
            }

            // Write the index...
            String propertiesClass = ProjectViewInternal.searchProperty(X_Namespace.PROPERTY_SCHEMA_PROPERTIES_INJECT, root);
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
            // if this happens more than once, we will blow up.
            // This is actually ideal, since we _really_ don't want two copies of indexes fighting over who is "right"
            root.getExtensions().add(X_Namespace.KEY_SCHEMA_PROPERTIES_ID, properties);
            final XapiSchemaParser parser = XapiSchemaParser.fromView(root);
            final DefaultSchemaMetadata metadata = parser.getSchema(explicitPlatform);
            if (settings.getRootProject().getName().isEmpty()) {
                logger.quiet("Configuring default root project name of file://{} to {}", settings.getRootDir(), metadata.getName());
                settings.getRootProject().setName(metadata.getName());
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
        final String rootDir = view.getProjectDir().getPath();
        final String maybeInclude;
        File xapiModern = new File(rootDir, "gradle/xapi-modern.gradle");
        if (xapiModern.exists()) {
            maybeInclude = "\n" +
                    "apply from: \"$rootDir/gradle/xapi-modern.gradle\"" +
                    "\n";
        } else {
            maybeInclude = "";
        }

        view.getLogger().info("All projects ({}):\n{}", map.getAllProjects().size(),
                map.getAllProjects().map(s->s.getPathGradle() + "@" + s.getPublishedName()).join("\n"));
        settings.getGradle().beforeProject(p->{
            if (!":".equals(p.getPath()) && map.hasGradleProject(p.getPath())) {
                p.getLogger().info("Setting project to modern mode {}", p.getPath());
                if (null == p.getExtensions().findByName("xapiModern")) {
                    p.getExtensions().add("xapiModern", "true");
                }
            }


            final Maybe<SchemaProject> match = map.getAllProjects()
                    .firstMatch(m -> {
                        return m.getPathGradle().equals(p.getPath());
                    });
            if (match.isPresent()) {
                // This is a gradle project who matches one of our schema-backed project declarations.
                // Lets setup some support utilities

                setupProject(view, p, match.get(), properties, index, map);
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
            logger.info("Processing xapi schema for project {}", project);
            String gradlePath = ":" + project.getSubPath().replace('/', ':');
            if (project != map.getRootProject()) {
                File dir = new File(settings.getSettingsDir(), project.getSubPath());
                if (project.isMultiplatform()) {
                    logger.quiet("Skipping multiplatorm root {} for project {}; virtual? {}", gradlePath, project, project.isVirtual());
                } else {
//                    gradlePath += "-main";
                    logger.quiet("Including {} for project {}; multiplatform? {} virtual? {}", gradlePath, project, project.isMultiplatform(), project.isVirtual());
                    rememberProject(settings, gradlePath);
                    settings.include(gradlePath);
                    if (dir.isDirectory()) {
                        final ProjectDescriptor p = settings.project(gradlePath);
                        if (new File(dir, project.getName() + ".gradle").exists()) {
                            p.setProjectDir(dir);
                            p.setBuildFileName(project.getName() + ".gradle");
                        } else if (new File(dir, project.getName() + ".gradle.kts").exists()) {
                            p.setProjectDir(dir);
                            p.setBuildFileName(project.getName() + ".gradle.kts");
                        }
                    }
                }
            }
            // create sub-projects; one for each platform/module pair.
            final int[] liveCnt = {0};
            final File aggregatorRoot = project.getView().getProjectDir();
            final String segment = project.getSubPath();
            final String gradlePrefix = gradlePath;
            final SetLike<String> liveNames = X_Jdk.setLinked();
            project.forAllPlatformsAndModules((plat, mod) -> {
                if (!project.isMultiplatform()) {
                    if (!plat.getName().equals(project.getDefaultPlatformName())) {
                        return;
                    }
                    if (!mod.getName().equals(project.getDefaultModuleName())) {
                        return;
                    }
                }
                String key = QualifiedModule.unparse(plat.getName(), mod.getName());
                String modKey = project.getName() + (project.isMultiplatform() ? "-" + key : "");
                String projectName = project.isMultiplatform() ? gradlePrefix + "-" + key : gradlePrefix;
                String moduleSourcePath = "src" + File.separator + key;
                String moduleTestSourcePath = "src" + File.separator + key + "Test";
                String gradleSourcePath = project.isMultiplatform() ? moduleSourcePath : "";
                String buildscriptSrcPath = "src/gradle/" + key;
                final File gradleSourceDir = new File(aggregatorRoot, gradleSourcePath);
                final File moduleSourceDir = new File(aggregatorRoot, moduleSourcePath);
                final File moduleTestSourceDir = new File(aggregatorRoot, moduleTestSourcePath);
                final File buildscriptSrc = new File(aggregatorRoot, buildscriptSrcPath);
                final String buildFileName = project.getName() + (project.isMultiplatform() ? GradleCoerce.toTitleCase(key) : "") + ".gradle";
                map.whenResolved(() -> {

                    IndexNodePool pool = map.getNodePool();
                    boolean isLive = index.hasEntries(view, project.getPathIndex(), plat, mod);
                    if (!isLive) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Not live: {}@{}-{}", project.getPathIndex(), plat, mod);
                        }
                    } else { // isLive == true
                        logger.quiet("Live: {}@{}-{}", project.getPathIndex(), plat, mod);
                        final File userBuildFile = new File(gradleSourceDir, buildFileName);
                        final PlatformModule myPlatMod = pool.getPlatformModule(plat.getName(), mod.getName());
                        liveNames.add(userBuildFile.getPath() + " - " + projectName + "/" + moduleSourcePath
                            + " - " + pool.getNode(pool.getIdentity(view, gradlePath, myPlatMod)));
                        if (++liveCnt[0] > 1) {
                            if (!project.isMultiplatform()) {
                                if (liveCnt[0] == 2) {
                                    // might be main and test?
                                }
                                throw new IllegalStateException("A project (" + project.getPathGradle() + ") with more than one live module must be multiplatform=true (cannot be standalone). " +
                                        "\nknown projects:\n" + liveNames.join(",\n"));
                            }
                        }
                        // to be able to avoid creating gradle projects we don't _need_,
                        // we'll check on the written index to decide whether to create said projects or not.
                        // user may freely create and check in their own projects, and we'll happily hook them up,
                        // so, by default, only schema.xapi / indexed dependencies can trigger a generated project creation.

                        final File generatedFile = new File(gradleSourceDir, "generated-" + buildFileName);

                        final String inclusion = "apply from: " +
                                "\"$rootDir/" +
                                segment + "/" +
                                (gradleSourcePath.isEmpty() ? "" : gradleSourcePath + "/")
                                + generatedFile.getName() + "\"";
                        final BuildScriptBuffer defaultContent = makeDefaultBuildScript(inclusion);
                        String defaultSource = defaultContent.toSource();
                        if (userBuildFile.exists()) {
                            String buildFileContents = GradleFiles.readFile(userBuildFile);
                            if (buildFileContents.isEmpty()) {
                                // empty file, we'll feel free to write into it.
                                logger.info("Replacing empty build script {} with default content", userBuildFile);
                            } else if (buildFileContents.contains(inclusion)) {
                                // this signals later code not to overwrite the file. User has changes.
                                defaultSource = "";
                                // need to mark this node as "has explicit buildscript"... much sooner than now!
                                // if (buildFileContents.equals(defaultSource)) { //...
                            } else if ("true".equals(view.findProperty("forceRegen")) || "all".equals(view.findProperty("force"))) {
                                File backup = new File(view.getProjectDir(), "build/buildscripts/backup-" + LocalDateTime.now().format(formatter) + "-" + userBuildFile.getName());
                                backup.getParentFile().mkdirs();
                                logger.quiet("User passed -Pforce=all or -PforceBuildscripts=true; forcibly overwriting user-owned submodule " + userBuildFile.getName());
                                logger.quiet("A backup of this file was created in " + backup.getAbsolutePath());
                                if (backup.exists()) {
                                    backup.delete();
                                }
                                userBuildFile.renameTo(backup);
                            } else {
                                throw new GradleException("Fatal error; file " + userBuildFile.getAbsolutePath() + " does not contain expected text: " + inclusion
                                    + "\nSet -PforceRegen=true or -Pforce=all to forcibly overwrite user-owned .gradle files (you should git commit first!)");
                            }
                        }
                        if (X_String.isNotEmpty(defaultSource)){
                            GradleFiles.writeFile(userBuildFile, defaultSource);
                        }


                        if (project.isMultiplatform()) {
                            view.getLogger().quiet("Multiplatform {} -> {} file://{} @ {}", projectName, modKey, userBuildFile, key);
                            if (dryRun(view)) {
                                view.getLogger().quiet("Dry run exiting early; skipping project creation for {} with build file {} for {}:{}", project.getPath(), userBuildFile.getAbsolutePath(), plat, mod);
                            } else {
//                                if (project.isVirtual()) {
                                view.getLogger().quiet("Adding multi-platform root {} -> {} file://{} @ {}", projectName, modKey, userBuildFile, key);
                                rememberProject(settings, projectName);
                                settings.include(projectName);
                                final ProjectDescriptor proj = settings.findProject(projectName);
                                proj.setProjectDir(gradleSourceDir);
                                proj.setBuildFileName(buildFileName);
                                proj.setName(modKey);
                                view.getLogger().info("Creating project {} named {} with build file {} for module {}:{}", projectName, modKey, userBuildFile.getAbsolutePath(), plat, mod);
//                                }
                            }
                        } else {
                            // intentionally not using Monoplatform; it blends into Multiplatform too easily in logs
                            view.getLogger().quiet("Singleplatform {} -> {} file://{} @ {}", project.getPathGradle(), modKey, userBuildFile, key);
                        }

                        // Create our generated buildscript containing dependencies, publication configuration or any other settings we want to handle automatically

                        BuildScriptBuffer out = new BuildScriptBuffer(false);

                        final In2Out1<String, Out1<Printable<?>>, Boolean> maybeAdd =
                                (name, buffer) -> {
                                    In2<String, File> cb = (script, file) ->
                                            buffer.out1()
                                                    .print("// GenInclude ").print(name).print(" from file://").println(file.getAbsolutePath())
                                                    .println(script)
                                            ;
                                    return maybeRead(buildscriptSrc, name, cb);
                                };
                        final Out1<Printable<?>> getBody = Immutable.immutable1(out);
                        if (buildscriptSrc.exists()) {
                            // Use src/gradle/key path to look for files to use to assemble a build script for us.
                            // since we don't need any such thing yet, just leaving a space here for it to be done later.

                            maybeRead(buildscriptSrc, "imports", (imports, file) -> {
                                for (String s : imports.split("[\\n\\r]+")) {
                                    out.addImport(s);
                                }
                            });
                            final Out1<Printable<?>> getBuildscript = out::getBuildscript;
                            maybeAdd.io("buildscript.start", getBuildscript);
                            maybeAdd.io("buildscript", getBuildscript);
                            maybeAdd.io("buildscript.end", getBuildscript);

                            final In2<String, File> addPlugin = (plugins, file) -> {
                                out.getPlugins().print("// GenInclude plugin from file://").println(file.getAbsolutePath());
                                for (String s : plugins.split("[\\n\\r]+")) {
                                    if (!s.startsWith("//")) {
                                        out.addPlugin(s);
                                    }
                                }
                                out.getPlugins().println();
                            };
                            maybeRead(buildscriptSrc, "plugins.start", addPlugin);
                            maybeRead(buildscriptSrc, "plugins", addPlugin);
                            maybeRead(buildscriptSrc, "plugins.end", addPlugin);

                            maybeAdd.io("body.start", getBody);
                            maybeAdd.io("body", getBody);
                        }

                        out.println("// GenStart " + getClass().getSimpleName());
                        out.getPlugins().println("ext.xapiModern = 'true'");
                        if (X_String.isNotEmpty(maybeInclude)) {
                            out.printlns(maybeInclude);
                        }
                        out
                                .println("String repo = project.findProperty(\"xapi.mvn.repo\")")
                                .print("if (repo) ").startClosure()
                                    .startClosure("repositories")
                                        .startClosure("maven")
                                            .println("name = 'xapiLocal'")
                                            .println("url = repo");

                        out.println("String javaPlugin = findProperty('xapi.java.plugin') ?: 'java-library'");
                        out.println("apply plugin: javaPlugin");
                        // TODO: pull this from schema.xapi
                        out.println("repositories.mavenCentral()");
                        // To start, setup the sourceset!
                        String srcSetName = project.isMultiplatform() ? "test".equals(key) ? "test" : "main" : key;
                        LocalVariable main = out.addVariable(SourceSet.class, srcSetName, true);
                        LocalVariable test = out.addVariable(SourceSet.class, "test", true);
                        main.setInitializerPattern("sourceSets.maybeCreate('$1')", srcSetName);
                        test.setInitializerPattern("sourceSets.maybeCreate('$1')", "test");
                        main.invoke("java.setSrcDirs($2)", "[]");
                        main.invoke("resources.setSrcDirs($2)", "[]");
                        test.invoke("java.setSrcDirs($2)", "[]");
                        test.invoke("resources.setSrcDirs($2)", "[]");

                        // lets have a look at the types of source dirs to decide what sourcesets to create.
                        if (moduleSourceDir.isDirectory()) {
                            for (String sourceDir : moduleSourceDir.list()) {
                                switch (sourceDir) {
                                    case "java":
                                    case "groovy":
                                    case "kotlin":
                                        main.access("java.srcDir(\"$2/$3\")",
                                                moduleSourceDir.getPath().replace(rootDir, "$rootDir"),
                                                sourceDir
                                        );
                                        break;
                                    case "resources":
                                        main.access("resources.srcDir(\"$2/resources\")",
                                                moduleSourceDir.getPath().replace(rootDir, "$rootDir")
                                        );
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
                        if (moduleTestSourceDir.isDirectory()) {
                            for (String sourceDir : moduleTestSourceDir.list()) {
                                switch (sourceDir) {
                                    case "java":
                                    case "groovy":
                                    case "kotlin":
                                        test.access("java.srcDir(\"$2/$3\")",
                                                moduleTestSourceDir.getPath().replace(rootDir, "$rootDir"),
                                                sourceDir
                                        );
                                        break;
                                    case "resources":
                                        test.access("resources.srcDir(\"$2/resources\")",
                                                moduleTestSourceDir.getPath().replace(rootDir, "$rootDir")
                                        );
                                        break;
                                    default:
                                        // perhaps warn about unused source directory? ...not really worth it.
                                }
                            }
                        }
                        Lazy<ClosureBuffer> dependencies = Lazy.deferred1(()->
                                out.startClosure("dependencies")
                        );
                        for (SchemaDependency dependency : project.getDependenciesOf(plat, mod)) {
                            view.getLogger().info("Processing dependency {}:{}:{} -> {}", project, plat, mod, dependency);

                            PlatformModule platMod = dependency.getCoords();

                            final IndexNode node;
                            final ModuleIdentity depIdent;
                            switch (dependency.getType()) {
                                case internal:
                                    final PlatformModule internalCoords = PlatformModule.parse(dependency.getName());
                                    depIdent = pool.getIdentity(view, project.getPathGradle(), internalCoords);
                                    node = pool.getNode(depIdent);
                                    break;
                                case project:
                                    String name = dependency.getName();
                                    depIdent = pool.getIdentity(view, name.startsWith(":") ? name : ":" + name, platMod);
                                    node = pool.getNode(depIdent);
                                    break;
                                default:
                                    depIdent = null;
                                    node = null;
                            }
                            boolean canSkip = dependency.getType() == DependencyType.internal; // just do internal for now.
                            if (canSkip && !index.dependencyExists(dependency, project, plat, mod)) {
                                // skipping a non-live dependency.
                                view.getLogger().info("Skipping non-live dependency {}:{}:{} -> {}", project, plat, mod, dependency);
                                continue;
                            }
                            if (canSkip && pool.isDeleted(depIdent)) {
                                view.getLogger().quiet("Skipping deleted dependency {}", depIdent);
                                continue;
                            }
                            if (canSkip && node != null && !node.isLive()) {
                                // when a node is not live, we should skip it if it has no compressed dependencies
//                                if (node.getCompressedDependencies().isEmpty()) {
//                                if (node.getAllDependencies().isEmpty()) {
//                                    view.getLogger().quiet("Skipping non-live module w/ no compressed dependencies {}", depIdent);
//                                    continue;
//                                }
                            }

                            final ClosureBuffer depOut = dependencies.out1();
                            switch (dependency.getTransitivity()) {
                                case api:
                                    if (out.addPlugin("java-library")) {
                                        out.getPlugins().print("// GenInclude ")
                                                .print(getClass().getSimpleName())
                                                .println(" adding java-library b/c api dependencies used");
                                    }
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "api" : key + "Api"
                                    );
                                    break;
                                case compile_only:
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "compileOnly" : key + "CompileOnly"
                                    );
                                    break;
                                case impl:
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "implementation" : key + "Implementation"
                                    );
                                    break;
                                case test:
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "testImplementation" : key + "TestImplementation"
                                    );
                                    break;
                                case runtime:
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "runtime" : key + "Runtime"
                                    );
                                    break;
                                case runtime_only:
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "runtimeOnly" : key + "RuntimeOnly"
                                    );
                                    break;
                                case annotation_processor:
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "annotationProcessor" : key + "AnnotationProcessor"
                                    );
                                    break;
                                default:
                                    throw new IllegalArgumentException("transitivity " + dependency.getTransitivity() + " is not supported!");
                            }
                            depOut.print(" ");
                            switch (dependency.getType()) {
                                case unknown:
                                    view.getLogger().warn("Unknown dependency type {}", dependency);
                                    break;
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
                                    // this is gross. need to consider deeply nested structures...
                                    if (index.isVirtual(view, path, coords)) {
                                        path = path + ":" + unparsed;
                                    } else if (index.isMultiPlatform(view, path, coords)) {
                                        // multi-platform needs to convert to a subproject dependency.
                                        path = path + "-" + unparsed;
                                    } else {
                                        // not multi-platform, we expect this to be a single-module dependency,
                                        // so, for now, no need to do anything; a plain project dependency is correct.
//                                    path = path + "\", configuration: \"" + unparsed + "Out";
                                        path = path.replace("-main", "");
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
                                    final PlatformModule internalCoords = PlatformModule.parse(dependency.getName());
//                                    String simpleName = path.substring(path.lastIndexOf(":") + 1);
                                    if (index.isMultiPlatform(view, project.getPathIndex(), internalCoords)) {
//                                        path = ":" + simpleName + "-" + platMod.toPlatMod();
                                        path = path + "-" + internalCoords.toPlatMod();

                                    }
                                    if (pool.isDeleted(depIdent)) {
                                        logger.quiet("Removing elided dependency " + depIdent);
                                        // when a dependency is deleted, we should instead absorb all of its non-deleted dependencies.
                                        depOut.print("/* elided " + path + " */ ");
                                        final IndexNode deletedDep = pool.getDeletedNode(depIdent);
                                        String prefix = "";
                                        for (IndexNode alive : deletedDep.getCompressedDependencies()) {
                                            depOut.print(prefix);
                                            final ModuleIdentity id = alive.getIdentity();
                                            String subPath = id.getProjectPath();
                                            String simpleSubName = path.substring(path.lastIndexOf(":") + 1);
                                            if (index.isMultiPlatform(view, id.getProjectPath(), id.getPlatformModule())) {
                                                subPath = id.getProjectPath() + ":" + simpleSubName + "-" + id.getPlatformModule().toPlatMod();
                                            }
                                            depOut.println("project(path: \"" + subPath + "\")");
                                            prefix = ",\n" + depOut.getIndent() + Printable.INDENT;
                                        }
                                        depOut.println();
                                    } else {
//                                        if (node.isLive()) {
                                            depOut.println("project(path: \"" + path + "\")");
//                                        } else {
//                                            depOut.print("/* compressed " + path + " */ ");
//                                            logger.quiet("Compressing non-live node {}", node.getIdentity());
//                                            final IndexNode nonLiveDep = pool.getNode(depIdent);
//                                            String prefix = "";
//
//                                            for (IndexNode alive : nonLiveDep.getCompressedDependencies()) {
//                                                depOut.print(prefix);
//                                                final ModuleIdentity id = alive.getIdentity();
//                                                String subPath = id.getProjectPath();
//                                                String simpleSubName = path.substring(path.lastIndexOf(":") + 1);
//                                                if (index.isMultiPlatform(view, id.getProjectPath(), id.getPlatformModule())) {
//                                                    subPath = id.getProjectPath() + ":" + simpleSubName + "-" + id.getPlatformModule().toPlatMod();
//                                                }
//                                                depOut.println("project(path: \"" + subPath + "\")");
//                                                prefix = ",\n" + depOut.getIndent() + Printable.INDENT;
//                                            }
//                                            depOut.println();
//
//                                        }
                                    }
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
                                .println(generatedFile.getPath().replace(settings.getSettingsDir().getPath(), "$rootDir"));
                        if (dryRun(view)) {
                            view.getLogger().info("Skipping write of generated build file due to dry run status");
                        } else {
                            // all done writing generated project
                            GradleFiles.writeFile(generatedFile, out.toSource());
                        }
                    }
                });
            });
        });
    }

    public static void rememberProject(final Settings settings, final String gradlePath) {
        InternalGradleCache.buildOnce(settings, "_xapiProjects", missing-> X_Jdk.listArrayConcurrent()).add(gradlePath);
    }
    public static boolean hasRememberedProject(final Settings settings, final String gradlePath) {
        return InternalGradleCache.buildOnce(settings, "_xapiProjects", missing-> X_Jdk.listArrayConcurrent()).containsEquality(gradlePath);
    }

    private BuildScriptBuffer makeDefaultBuildScript(final String inclusion) {
        BuildScriptBuffer b = new BuildScriptBuffer();
        b.println("// This buildscript was generated as a place for you to manually configure your subproject as you wish.");
        b.println("//");
        b.println("// You can safely delete this file, and a new one will be generated for you in its place");
        b.println("//");
        b.println("// The only rule is that, somewhere in this file, even in a comment, you must include the following line:");
        b.println(inclusion);
        b.println("//");
        b.println("// Feel free to 'own' this script, and do with it as you please, just make sure you include the generated script at some point.");
        return b;
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

    private void setupProject(final RootProjectView view, final Project gradleProject, final SchemaProject project, final SchemaProperties properties, final SchemaIndexReader index, final SchemaMap map) {
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
        BuildCoordinates coordinates = RootProjectView.rootView(gradleProject.getGradle());
        if (project.isMultiplatform()) {
            // If we are multi-platform, then we have sub-projects for each of our modules.
            // These subprojects will not match the SchemaProject, nor will they need to:
            // these subprojects contain generated dependency / general wiring;
            // any API-sugar we sprinkle on top goes into the multiplatform aggregator (the parent of all modules)
            project.forAllPlatformsAndModules((plat, mod) -> {
                if (index.hasEntries(coordinates, project.getPathIndex(), plat, mod)) {
                    gradleProject.getLogger().quiet("Found multiplatform combination {}-{} for {} with gradle path {}",
                            plat.getName(), mod.getName(), project.getPathGradle(), gradleProject.getPath());
                    String suffix = PlatformModule.unparse(plat.getName(), mod.getName());
                    String path = project.getPathGradle() + "-" + suffix;
                    final ExtensionContainer ext = gradleProject.findProject(path).getExtensions();
                    if (ext.findByName("xapiModern") == null) {
                        logger.quiet("Making modern: " + path + " - " + gradleProject.getPath());
                        ext.add("xapiModern", "true");
                    } else {
                        logger.quiet("Already modern: " + path);
                    }
                }
            });

        } else {
            // A non-multi-platform project needs to have a bit more "hidden surgery".
            project.forAllPlatformsAndModules((plat, mod) -> {

                if (index.hasEntries(coordinates, project.getPathIndex(), plat, mod)) {
                    // for now, lets apply our generated gradle source...
                    // if this gets too yucky, we can just replace it w/ direct (but hidden) "act on Project" object code
                    if (dryRun(view)) {
                        gradleProject.getLogger().quiet("Found index entry for {} with index path {}", project.getPathGradle(), project.getPathIndex());
                        return;
                    }
                    String key = QualifiedModule.unparse(plat.getName(), mod.getName());
                    String path = gradleProject.getProjectDir().getAbsolutePath() +
//                            File.separator + "mods" + File.separator + key + File.separator +
                            (project.isMultiplatform() ? File.separator + "src" + File.separator + key : "") +
                            File.separator + project.getName() + (project.isMultiplatform() ? X_String.toTitleCase(key) : "") + ".gradle";
                    if (new File(path + ".kts").exists()) {
                        path = path + ".kts";
                    }
                    final String finalPath = path;
                    gradleProject.getLogger().quiet("{} is sourcing generated path {}", gradleProject.getPath(), finalPath);
//                    gradleProject.apply(o-> {
//                        o.from(finalPath);
//                    });
                }
            });
        }
    }

    private boolean dryRun(MinimalProjectView view) {
        return
                false;
//                !"false".equals(System.getProperty("xapiDryRun")) &&
//                !"false".equals(view.findProperty("xapiDryRun"));
    }
    private boolean maybeRead(final File dir, final String file, final In2<String, File> callback) {
        File target = new File(dir, file);
        if (target.isFile()) {
            final String contents = GradleFiles.readFile(target);
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

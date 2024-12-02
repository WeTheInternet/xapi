package net.wti.gradle.settings.plugin;

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
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.SourceSet;
import xapi.constants.X_Namespace;
import xapi.dev.source.BuildScriptBuffer;
import xapi.dev.source.ClosureBuffer;
import xapi.dev.source.LocalVariable;
import xapi.fu.*;
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
            .appendPattern(X_Namespace.TIMESTAMP_FORMAT_NO_TZ)
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

        view.getLogger().info("All projects ({}):\n{}", map.getAllProjects().size(),
                map.getAllProjects().map(s->s.getPathGradle() + "@" + s.getPublishedName()).join("\n"));
//
//        // All the mess below is "plugin-scoped initialization", and amounts to ~nothing important.
//        // It should all probably just be deleted, and replaced w/ user-visible codegen.
//        settings.getGradle().beforeProject(p->{
//            if (!":".equals(p.getPath()) && map.hasGradleProject(p.getPath())) {
//                p.getLogger().info("Setting project to modern mode {}", p.getPath());
//                if (null == p.getExtensions().findByName("xapiModern")) {
//                    p.getExtensions().add("xapiModern", "true");
//                }
//            }
//
//
//            final Maybe<SchemaProject> match = map.getAllProjects()
//                    .firstMatch(m -> {
//                        return m.getPathGradle().equals(p.getPath());
//                    });
//            if (match.isPresent()) {
//                // This is a gradle project who matches one of our schema-backed project declarations.
//                // Lets setup some support utilities
//
//                setupProject(view, p, match.get(), properties, index, map);
//            }
//        });
//        settings.getGradle().afterProject(p->{
//            final Maybe<SchemaProject> match = map.getAllProjects()
//                    .firstMatch(m -> m.getPathGradle().equals(p.getPath()));
//            if (match.isPresent()) {
//                // This is a gradle project who matches one of our schema-backed project declarations.
//                // Lets setup some support utilities
//
//                p.getLogger().trace("Matched project {}", p.getBuildTreePath());
//                finalizeProject(p, match.get(), properties, index, map);
//            } else {
//                // warn about a missing project
//                p.getLogger().trace("Unmatched project {}", p.getBuildTreePath());
//            }
//        });

        map.getCallbacks().perProject(project -> {
            logger.info("Processing xapi schema for project {}", project);
            String gradlePath = ":" + project.getSubPath().replace('/', ':');
            if (project != map.getRootProject()) {
                File dir = new File(settings.getSettingsDir(), project.getSubPath());
                if (project.isMultiplatform()) {
                    logger.info("Skipping multi-platform root {} for project {}; virtual? {}", gradlePath, project, project.isVirtual());
                } else {
//                    gradlePath += "-main";
                    logger.info("Including {} for project {}; multiplatform? {} virtual? {}", gradlePath, project, project.isMultiplatform(), project.isVirtual());
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
                String moduleTestSourcePath = "src" + File.separator + (project.isMultiplatform() && !"main".equals(key) ? key + "Test" : "test");
                String gradleSourcePath = project.isMultiplatform() ? moduleSourcePath : "";
                String buildscriptSrcPath = "src/gradle/" + key;
                String sourceModulePath = gradleSourcePath + "/build/srcMod" + X_String.toTitleCase(key);
                final File gradleSourceDir = new File(aggregatorRoot, gradleSourcePath);
                final File sourceModuleDir = new File(aggregatorRoot, sourceModulePath);
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
                        logger.info("Live: {}@{}-{}", project.getPathIndex(), plat, mod);
                        final File userBuildFile = new File(gradleSourceDir, buildFileName);
                        final File sourceBuildFile = new File(sourceModuleDir, buildFileName.replace(".gradle", "-sources.gradle"));
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

                        final File lastGeneratedFile = new File(gradleSourceDir, "build/buildscripts/" + buildFileName);
                        lastGeneratedFile.getParentFile().mkdirs();

                        if (userBuildFile.exists()) {
                            String buildFileContents = GradleFiles.readFile(userBuildFile);
                            if (lastGeneratedFile.exists()) {
                                String lastGenContents = GradleFiles.readFile(lastGeneratedFile);
                                if (!lastGenContents.equals(buildFileContents)) {
                                    if ("true".equals(view.findProperty("forceRegen")) || "all".equals(view.findProperty("force"))) {
                                        File backup = new File(view.getProjectDir(), "build/buildscripts/backup-" + LocalDateTime.now().format(formatter) + "-" + userBuildFile.getName());
                                        backup.getParentFile().mkdirs();
                                        logger.quiet("User passed -Pforce=all or -PforceBuildscripts=true; forcibly overwriting user-owned submodule " + userBuildFile.getName());
                                        logger.quiet("A backup of this file was created in " + backup.getAbsolutePath());
                                        if (backup.exists()) {
                                            backup.delete();
                                        }
                                        userBuildFile.renameTo(backup);
                                    } else {
                                        throw new GradleException("Manual changed to generated build file " + userBuildFile.getPath() + " detected! To see difference, run:\n" +
                                                "diff " + userBuildFile.getPath() + " " + lastGeneratedFile.getPath() + "\n" +
                                                "Any changes to buildscripts need to be placed into " + buildscriptSrc.getPath() + "/{body.end,body.start,plugins,buildscript} file(s).\n" +
                                                "Set -PforceRegen=true or -Pforce=all to forcibly overwrite user-owned .gradle files (you should git commit first!)");
                                    }
                                }
                            }
                        }


                        if (project.isMultiplatform()) {
                            view.getLogger().info("Multiplatform {} -> {} file://{} @ {}", projectName, modKey, userBuildFile, key);
//                            if (project.isVirtual()) {
                            view.getLogger().info("Adding multi-platform root {} -> {} file://{} @ {}", projectName, modKey, userBuildFile, key);
                            rememberProject(settings, projectName);
                            settings.include(projectName);
                            final ProjectDescriptor proj = settings.findProject(projectName);
                            proj.setProjectDir(gradleSourceDir);
                            proj.setBuildFileName(buildFileName);
                            proj.setName(modKey);
                            view.getLogger().info("Creating project {} named {} with build file {} for module {}:{}", projectName, modKey, userBuildFile.getAbsolutePath(), plat, mod);
//                            }
                        } else {
                            // intentionally not using Monoplatform; it blends into Multiplatform too easily in logs
                            view.getLogger().info("Singleplatform {} -> {} file://{} @ {}", project.getPathGradle(), modKey, userBuildFile, key);
                        }

                        // Create our generated buildscript containing dependencies, publication configuration or any other settings we want to handle automatically

                        BuildScriptBuffer out = new BuildScriptBuffer(true);
                        BuildScriptBuffer srcMod = new BuildScriptBuffer(true);

                        final In2Out1<String, Out1<Printable<?>>, Boolean> maybeAdd =
                                (name, buffer) -> {
                                    In2<String, File> cb = (script, file) -> {
                                            buffer.out1()
                                                    .print("// GenInclude ").print(name).print(" from file://").println(file.getAbsolutePath())
                                                    .printlns(script);
                                    }
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

                        out.println("// GenStart " + getClass().getName());
                        out.println("ext.xapiModern = 'true'");
                        // for now, we're just going to forcibly use java-library.
                        // we can add an optional switch to disable this if it's ever needed
                        out.addPlugin("java-library");
                        // Need to make the java version configurable...
                        out.println("java.toolchain.languageVersion = JavaLanguageVersion.of(8)");

                        // TODO: pull this from schema.xapi
                        out.println("repositories.mavenCentral()");
                        // To start, setup the sourceset!
                        String srcSetName = project.isMultiplatform() ? "test".equals(key) ? "test" : "main" : key;
                        out.println("// setup sourcesets");
                        LocalVariable main = out.addVariable(SourceSet.class, srcSetName, true);
                        LocalVariable test = out.addVariable(SourceSet.class, "test", true);
                        main.setInitializerPattern("sourceSets.maybeCreate('$1')", srcSetName);
                        test.setInitializerPattern("sourceSets.maybeCreate('$1')", "test");
                        main.invoke("java.setSrcDirs($2)", "[]");
                        main.invoke("resources.setSrcDirs($2)", "[]");
                        test.invoke("java.setSrcDirs($2)", "[]");
                        test.invoke("resources.setSrcDirs($2)", "[]");

                        final boolean isSourcePublished = plat.isSourcePublished(),
                                      isSourceConsumed = plat.isSourceConsumed();
                        if (isSourcePublished) {
                            // need to setup a synthetic source module, so we can include source transitivity
                            view.getLogger().info("Setting up transitive source project for {}", userBuildFile);
                            srcMod.addPlugin("java-library");
                            srcMod.addPlugin("maven-publish");
                        }

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
                        Lazy<ClosureBuffer> dependenciesSources = Lazy.deferred1(()->
                                srcMod.startClosure("dependencies")
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
                            boolean withSource = false;
                            switch (dependency.getTransitivity()) {
                                case api:
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "api" : key + "Api"
                                    );
                                    if (isSourcePublished) {
                                        withSource = true;
                                        dependenciesSources.out1().print("api ");
                                    }
                                    break;
                                case compile_only:
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "compileOnly" : key + "CompileOnly"
                                    );
                                    if (isSourcePublished) {
                                        withSource = true;
                                        if (dependency.getType() == DependencyType.external) {
                                            dependenciesSources.out1().print("compileOnly ");
                                        } else {
                                            dependenciesSources.out1().print("api ");
                                        }
                                    }
                                    break;
                                case impl:
                                    depOut.print(
                                            project.isMultiplatform() || "main".equals(key) ?
                                                    "implementation" : key + "Implementation"
                                    );
                                    if (isSourcePublished) {
                                        withSource = true;
                                        if (dependency.getType() == DependencyType.external) {
                                            dependenciesSources.out1().print("implementation ");
                                        } else {
                                            dependenciesSources.out1().print("api ");
                                        }
                                    }
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
                            PlatformModule dependencyCoords = null;
                            switch (dependency.getType()) {
                                case unknown:
                                    view.getLogger().warn("Unknown dependency type {}", dependency);
                                    break;
                                case project:
                                    String path = dependency.getName();
                                    if (!path.startsWith(":")) {
                                        path = ":" + path;
                                    }
                                    dependencyCoords = dependency.getCoords();
                                    if (dependencyCoords.getPlatform() == null) {
                                        dependencyCoords = dependencyCoords.edit(plat.getName(), null);
                                    }
                                    if (dependencyCoords.getModule() == null) {
                                        dependencyCoords = dependencyCoords.edit(null, mod.getName());
                                    }
                                    // compute whether the target project is not-multiplatform / needs configuration: set too.
                                    final String unparsed = QualifiedModule.unparse(dependencyCoords);
                                    // this is gross. need to consider deeply nested structures...
                                    if (index.isVirtual(view, path, dependencyCoords)) {
                                        path = path + ":" + unparsed;
                                    } else if (index.isMultiPlatform(view, path, dependencyCoords)) {
                                        // multi-platform needs to convert to a subproject dependency.
                                        path = path + "-" + unparsed;
                                    } else {
                                        // not multi-platform, we expect this to be a single-module dependency,
                                        // so, for now, no need to do anything; a plain project dependency is correct.
//                                    path = path + "\", configuration: \"" + unparsed + "Out";
                                        path = path.replace("-main", "");
                                    }
                                    depOut.println("project(path: \"" + path + "\")");
                                    if (withSource) {
                                        dependenciesSources.out1().println("project(path: \"" + path + "-sources\")");
                                    }
                                    break;
                                case internal:
                                    // to have an internal dependency is to inherently be multiplatform
                                    // (there is no distinction between platform and module when it comes to "do you have more than one module")
                                    path = project.getPathGradle();
                                    if (":".equals(path)) {
                                        path = project.getName();
                                    }
                                    dependencyCoords = PlatformModule.parse(dependency.getName());
//                                    String simpleName = path.substring(path.lastIndexOf(":") + 1);
                                    if (index.isMultiPlatform(view, project.getPathIndex(), dependencyCoords)) {
//                                        path = ":" + simpleName + "-" + platMod.toPlatMod();
                                        path = path + "-" + dependencyCoords.toPlatMod();

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
                                            if (withSource) {
                                                dependenciesSources.out1().println("project(path: \"" + subPath + "-sources\")");
                                            }
                                            prefix = ",\n" + depOut.getIndent() + Printable.INDENT;
                                        }
                                        depOut.println();
                                    } else {
//                                        if (node.isLive()) {
                                            depOut.println("project(path: \"" + path + "\")");
                                            if (withSource) {
                                                dependenciesSources.out1().println("project(path: \"" + path + "-sources\")");
                                            }
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
                                    // consider looking up externals in xindex to see if/how we should add sources dependencies
                                    if (withSource) {
                                        if (index.hasExternalSources(dependency)) {
                                            dependenciesSources.out1().println("\"" + dependency.getGNV() + ":sources\"");
                                        } else {
                                            // the default operation is to just re-include external dependencies
                                            dependenciesSources.out1().println("\"" + dependency.getGNV() + "\"");
                                        }
                                    }
                                    continue;
                            } // end switch(dep.getType())
                        } // end for (dep :

                        // now, lets add some publishing!
                        String pubName = project.getPublishedName();
                        String groupNameResolved = properties.resolvePattern(plat.getPublishPattern(), fullIndex, pubName, plat.getName(), "");;
                        String modNameResolved = properties.resolvePattern(mod.getPublishPattern(), fullIndex, pubName, plat.getName(), mod.getName());
                        String mavenRepo = (String) view.findProperty("xapi.mvn.repo");
                        if (X_String.isNotEmpty(mavenRepo)) {
                            out
                                .startClosure("repositories")
                                .startClosure("maven")
                                .println("name = 'xapiLocal'")
                                .println("url = \"" + mavenRepo + "\"");
                            if (isSourcePublished) {
                                srcMod
                                    .startClosure("repositories")
                                    .startClosure("maven")
                                    .println("name = 'xapiLocal'")
                                    .println("url = \"" + mavenRepo + "\"");

                            }
                        }
                        if (index.isPublished(view, project.getPathIndex(), myPlatMod)) {
                            out.addPlugin("maven-publish");
                            if (X_String.isEmpty(mavenRepo)) {
                                mavenRepo = "$rootDir/repo";
                            }
                            final String commonPub =
                                    "project.extensions.add('xapi.mvn.repo', \"" + mavenRepo + "\")\n" +
                                    "Task xapiPublish = tasks.create('xapiPublish')\n" +
                                    "xapiPublish.group = 'Publishing'\n" +
                                    "xapiPublish.description = 'Publish jars to xapiLocal repository'\n" +
                                    "xapiPublish.dependsOn \"publishXapiPublicationToXapiLocalRepository\"\n" +
                                    "\n" +
                                    "PublishingExtension ext = extensions.findByName(PublishingExtension.NAME) as PublishingExtension\n" +
                                    "ext.repositories.maven {\n" +
                                    "    MavenArtifactRepository repo ->\n" +
                                    "        repo.name = 'xapiLocal'\n" +
                                    "        repo.url = \"file://" + mavenRepo + "\"\n" +
                                    "}\n" +
                                    "// not really what we want, but it's good enough for right now\n" +
                                    "java.withSourcesJar()\n" +
                                    "java.withJavadocJar()\n" +
                                    "\n" +
                                    "ext.publications.create(\"xapi\", MavenPublication, {\n" +
                                    "    pub ->\n" +
                                    "        afterEvaluate {\n" +
                                    "            if (tasks.names.contains('shadowJar')) {\n" +
                                    "                shadow.component(pub)\n" +
                                    "            } else {\n" +
                                    "                pub.from(components.named('java').get())\n" +
                                    "            }\n" +
                                    "        }\n";
                            out
                                .println("// Setup publishing to coordinates: " + groupNameResolved + ":" + modNameResolved)
                                .printlns(commonPub +
                                    "        pub.artifactId = \"" + modNameResolved + "\"\n" +
                                    "        pub.groupId = \"" + groupNameResolved + "\"\n" +
                                    "})\n"
                                );
                            if (isSourcePublished) {
                                srcMod.printlns(
                                        "File allSrcDir = layout.buildDirectory.dir(\"allSrc\").get().asFile\n" +
                                        "TaskProvider<Sync> copySource = tasks.register('copySource', Sync).configure {\n" +
                                        "    Sync s ->\n" +
                                        "        s.destinationDir = allSrcDir\n" +
                                        "        s.from(project.provider({\n" +
                                        "            project(\"" + projectName + "\").sourceSets.main.allSource.sourceDirectories\n" +
                                        "        }))\n" +
                                        "}\n" +
                                        "sourceSets.main.java.setSrcDirs([])\n" +
                                        "sourceSets.main.resources.setSrcDirs([])\n" +
                                        "sourceSets.main.resources.srcDir(allSrcDir)\n" +
                                        "java.withSourcesJar()\n" +
                                        "tasks.named('processResources').configure {\n" +
                                        "  dependsOn \"copySource\"\n" +
                                        "}\n" +
                                        "tasks.named('sourcesJar').configure {\n" +
                                        "  dependsOn \"copySource\"\n" +
                                        "}\n");
                                srcMod.println("// Setup source publishing to coordinates: " + groupNameResolved + ":" + modNameResolved)
                                .printlns(commonPub +
                                    "        pub.artifactId = \"" + modNameResolved + "-sources\"\n" +
                                    "        pub.groupId = \"" + groupNameResolved + "\"\n" +
                                    "})\n"
                                );
                            }

                        } else {
                            // we'll want to reduce this warn() to info() later on; for now, all our modules are published,
                            // and we want to know if something slips through.
                            logger.warn("Skipping publishing: {}@{}", project.getPathIndex(), myPlatMod);
                        }

                        if (isSourceConsumed) {
                            dependencies.out1().println("api project(\"" + projectName + "-sources\")");
                        }

                        out.println("// GenEnd " + getClass().getName());

                        maybeAdd.io("body.end", getBody);
                        out.print("// Done generating buildfile for ")
                                .print(project.getPathGradle())
                                .print(" at file://")
                                .println(userBuildFile.getPath().replace(settings.getSettingsDir().getPath(), "$rootDir"));
                        // all done writing generated project
                        final String finalSrc = out.toSource();
                        GradleFiles.writeFile(userBuildFile, finalSrc);
                        GradleFiles.writeFile(lastGeneratedFile, finalSrc);
                        if (isSourcePublished) {
                            final String finalSrcMod = srcMod.toSource();
                            GradleFiles.writeFile(sourceBuildFile, finalSrcMod);
                            settings.include(projectName + "-sources");
                            final ProjectDescriptor sourceMod = settings.project(projectName + "-sources");
                            sourceMod.setProjectDir(sourceBuildFile.getParentFile());
                            sourceMod.setBuildFileName(sourceBuildFile.getName());
                            // need to also include() this module...
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

//    private void finalizeProject(final Project gradleProject, final SchemaProject project, final SchemaProperties properties, final SchemaIndexReader index, final SchemaMap map) {
//        if (!project.isMultiplatform()) {
//            gradleProject.getLogger().quiet("Finalizing {}. Configurations: {}", gradleProject.getPath(), gradleProject.getConfigurations().getNames());
//            gradleProject.getGradle().projectsEvaluated(g->{
//                gradleProject.apply(o-> {
//                    gradleProject.getLogger().info("Finalizing {}. Configurations: {}", gradleProject.getPath(), gradleProject.getConfigurations().getNames());
////                    o.plugin(XapiParserPlugin.class);
//                });
//            });
//        }
//    }
//
//    private void setupProject(final RootProjectView view, final Project gradleProject, final SchemaProject project, final SchemaProperties properties, final SchemaIndexReader index, final SchemaMap map) {
//        if (!project.isMultiplatform()) {
//            // a multi-platform project will make the parent module an aggregator for client modules.
//            // single-platform project will leave the main build.gradle file as the "main" module, and uses java-library plugin.
//            gradleProject.apply(o-> {
//                o.plugin(JavaLibraryPlugin.class);
//            });
//        }
////        final ProjectView view = ProjectView.fromProject(gradleProject);
////        view.getLogger().quiet("Setting up schema for project {}", view.getPath());
//
//        ListLike<SchemaDependency> externals = X_Jdk.list();
////        project.getDependencies().forEachPair((mod, dep) -> {
////            switch (dep.getType()) {
////                case external:
////                    externals.add(dep);
////                    break;
////            }
////        });
//        if (externals.isNotEmpty()) {
////            view.getTasks().register("resolveExternals", ResolveExternalsTask.class);
////            TaskSpy.spy(view, "resolveExternals", ResolveExternalsTask.class, resolve->{
////                // make all classpaths / configuration resolution depend on resolveExternals.
////            });
//            gradleProject.getLogger().quiet("Found externals {} for project {} with index path {}", externals, project.getPathGradle(), project.getPathIndex());
//        }
//        BuildCoordinates coordinates = RootProjectView.rootView(gradleProject.getGradle());
//        if (project.isMultiplatform()) {
//            // If we are multi-platform, then we have sub-projects for each of our modules.
//            // These subprojects will not match the SchemaProject, nor will they need to:
//            // these subprojects contain generated dependency / general wiring;
//            // any API-sugar we sprinkle on top goes into the multiplatform aggregator (the parent of all modules)
//            project.forAllPlatformsAndModules((plat, mod) -> {
//                if (index.hasEntries(coordinates, project.getPathIndex(), plat, mod)) {
//                    gradleProject.getLogger().info("Found multiplatform combination {}-{} for {} with gradle path {}",
//                            plat.getName(), mod.getName(), project.getPathGradle(), gradleProject.getPath());
//                    String suffix = PlatformModule.unparse(plat.getName(), mod.getName());
//                    String path = project.getPathGradle() + "-" + suffix;
//                    final ExtensionContainer ext = gradleProject.findProject(path).getExtensions();
//                    if (ext.findByName("xapiModern") == null) {
//                        logger.quiet("Making modern: " + path + " - " + gradleProject.getPath());
//                        ext.add("xapiModern", "true");
//                    } else {
//                        logger.info("Already modern: " + path);
//                    }
//                }
//            });
//
//        } else {
//            // A non-multi-platform project needs to have a bit more "hidden surgery".
//            project.forAllPlatformsAndModules((plat, mod) -> {
//
//                if (index.hasEntries(coordinates, project.getPathIndex(), plat, mod)) {
//                    // apply our generated gradle source...
//                    String key = QualifiedModule.unparse(plat.getName(), mod.getName());
//                    String path = gradleProject.getProjectDir().getAbsolutePath() +
////                            File.separator + "mods" + File.separator + key + File.separator +
//                            (project.isMultiplatform() ? File.separator + "src" + File.separator + key : "") +
//                            File.separator + project.getName() + (project.isMultiplatform() ? X_String.toTitleCase(key) : "") + ".gradle";
//                    if (new File(path + ".kts").exists()) {
//                        path = path + ".kts";
//                    }
//                    final String finalPath = path;
//                    gradleProject.getLogger().info("{} is sourcing generated path {}", gradleProject.getPath(), finalPath);
////                    gradleProject.apply(o-> {
////                        o.from(finalPath);
////                    });
//                }
//            });
//        }
//    }

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

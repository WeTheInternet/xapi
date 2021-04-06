package net.wti.loader.plugin;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.loader.impl.BuildScriptBuffer;
import net.wti.gradle.loader.impl.ClosureBuffer;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.QualifiedModule;
import net.wti.gradle.schema.api.SchemaDependency;
import net.wti.gradle.schema.index.SchemaIndexReader;
import net.wti.gradle.schema.index.SchemaIndexerImpl;
import net.wti.gradle.schema.map.SchemaMap;
import net.wti.gradle.schema.parser.DefaultSchemaMetadata;
import net.wti.gradle.schema.parser.SchemaParser;
import net.wti.gradle.schema.spi.SchemaIndex;
import net.wti.gradle.schema.spi.SchemaProperties;
import net.wti.gradle.settings.ProjectDescriptorView;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.GFileUtils;
import xapi.dev.source.LocalVariable;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.util.X_Namespace;
import xapi.util.X_String;

import javax.inject.Inject;
import java.io.File;

import static net.wti.gradle.settings.ProjectDescriptorView.fromSettings;

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
        if (schema.exists()) {
            final ProjectDescriptorView root = fromSettings(settings);
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

            // Transverse the full */*/schema.xapi hierarchy
            final SchemaMap map = buildMap(settings, parser, metadata);

            // Write the index...
            String propertiesClass = SchemaProperties.searchProperty(X_Namespace.PROPERTY_SCHEMA_PROPERTIES_INJECT, root);
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
            SchemaIndexerImpl indexer = new SchemaIndexerImpl(properties);
            // TODO: derive buildName from a configurable Property<String>
            final Out1<SchemaIndex> index = indexer.index(
                root,
                properties.getBuildName(root, metadata),
                root.getProjectDir()
            );


            root.whenReady(view -> {
                // resolve the block-until-index-done task
                index.out1();
                // Setup callback for each project to add buildable / publishable component with multiple modules / platforms.
                prepareProjects(view, properties, settings, map);
                // Flush out callbacks in the priorities they were declared
                map.getCallbacks().flushCallbacks(map);
            });

        }

    }

    protected SchemaMap buildMap(
        Settings settings,
        SchemaParser parser,
        DefaultSchemaMetadata metadata
    ) {
        //noinspection UnnecessaryLocalVariable (nice for debugging)
        SchemaMap map = SchemaMap.fromView(parser.getView(), parser, metadata);
        return map;
    }

    private void prepareProjects(final MinimalProjectView view, final SchemaProperties properties, Settings settings, SchemaMap map) {
        SchemaIndexReader index = new SchemaIndexReader(properties, view, map);

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
            // Now, check if this project is configured to be multi-project...
            if (project.isMultiplatform()) {
                // create sub-projects; one for each platform/module pair.
                project.forAllPlatformsAndModules((plat, mod) -> {
                    String key = QualifiedModule.unparse(plat.getName(), mod.getName());
                    String projectName = gradlePath + (gradlePath.endsWith(":") ? "" : ":") + key;
                    String projectSrcPath = "src/gradle/" + key;
                    String projectOutputPath = "mods/" + key;

                    if (index.hasEntries(view, project.getName(), plat, mod)) {
                        // to be able to avoid creating gradle projects we don't _need_,
                        // we'll check on the written index to decide whether to create said projects or not.
                        // user may freely create and check in their own projects, and we'll happily hook them up,
                        // so, by default, only schema.xapi / indexed dependencies can trigger a generated project creation.
                        settings.include(projectName);
                        final ProjectDescriptor proj = settings.findProject(projectName);
                        final File projectRoot = project.getView().getProjectDir();
                        final File projDir = new File(projectRoot, projectOutputPath);
                        proj.setProjectDir(projDir);
                        String buildFileName = project.getName() + GradleCoerce.toTitleCase(key) + ".gradle";
                        if (new File(projDir, buildFileName + ".kts").exists()) {
                            buildFileName = buildFileName + ".kts";
                        }
                        File buildFile = new File(projDir, buildFileName);
                        proj.setBuildFileName(buildFileName);
                        File projectSource = new File(projectRoot, projectSrcPath);

                        // TODO: BuildScriptBuffer
                        BuildScriptBuffer out = new BuildScriptBuffer();
                        if (projectSource.exists()) {
                            // Use src/gradle/key path to look for files to use to assemble a build script for us.
                            // since we don't need any such thing yet, just leaving a space here for it to be done later.

                            maybeRead(projectSource, "imports", imports -> {
                                for (String s : imports.split("[\\n\\r]+")) {
                                    out.addImport(s);
                                }
                            });
                            final In1<String> addBuildscript = imports ->
                                    out.getBuildscript().printlns(imports);
                            maybeRead(projectSource, "buildscript.start", addBuildscript);
                            maybeRead(projectSource, "buildscript", addBuildscript);
                            maybeRead(projectSource, "buildscript.end", addBuildscript);
                            final In1<String> addPlugin = imports -> {
                                for (String s : imports.split("[\\n\\r]+")) {
                                    out.addPlugin(s);
                                }
                            };
                            maybeRead(projectSource, "plugins.start", addPlugin);
                            maybeRead(projectSource, "plugins", addPlugin);
                            maybeRead(projectSource, "plugins.end", addPlugin);

                            final In1<String> addBody = out::printlns;
                            maybeRead(projectSource, "body.start", addBody);
                            maybeRead(projectSource, "body", addBody);
                            maybeRead(projectSource, "body.end", addBody);
                        }
                        // To start, setup the sourceset!
                        String pv = out.addImport(ProjectView.class);
                        String fromProject = out.addImportStatic(ProjectView.class, "fromProject");
//                        ProjectView v = null; // just here to check autocomplete
                        out.println(pv + " view = " + fromProject + "(project)");
                        LocalVariable main = out.addVariable(SourceSet.class, "main", true);
                        main.setInitializer("view.sourceSets.maybeCreate('main')");

                        // lets have a look at the types of source dirs to decide what sourcesets to create.
                        File moduleSource = new File(projectRoot, "src" + File.separator + key);
                        if (moduleSource.isDirectory()) {
                            for (String sourceDir : moduleSource.list()) {
                                switch (sourceDir) {
                                    case "java":
                                    case "groovy":
                                    case "kotlin":
                                        main.access("java.srcDir(\"$2/$3\")", moduleSource.getAbsolutePath(), sourceDir);
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
                            final ClosureBuffer depOut = dependencies.out1();
                            switch (dependency.getTransitivity()) {
                                case api:
                                    out.addPlugin("java-library");
                                    depOut.append("api ");
                                    break;
                                case compile_only:
                                    depOut.append("compileOnly ");
                                    break;
                                case impl:
                                    depOut.append("implementation ");
                                    break;
                                case runtime:
                                    depOut.append("runtime ");
                                    break;
                                case runtime_only:
                                    depOut.append("runtimeOnly ");
                                    break;
                                default:
                                    throw new IllegalArgumentException("transitivity " + dependency.getTransitivity() + " is not supported!");
                            }
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
                                    if (index.isMultiPlatform(view, path)) {
                                        // multi-platform needs to convert to a subproject dependency.
                                        path = path + ":" + unparsed;
                                    } else {
                                        // not multi-platform, we need to depend on sane configuration
                                        path = path + "\" configuration: \"" + unparsed + "Out";
                                    }
                                    depOut.println("project(path: \"" + path + "\")");
                                    break;
                                case internal:
                                    path = gradlePath + (gradlePath.endsWith(":") ? "" : ":") + dependency.getName();
                                    depOut.println("project(path: \"" + path + "\")");
                                    break;
                                case external:
                                    depOut.println("\"" + dependency.getGNV() + "\"");
                                    break;
                            }
                        }


                        // all done writing generated project
                        GFileUtils.writeFile(out.toSource(), buildFile, "UTF-8");
                    }
                });
            }
        });
    }

    private void maybeRead(final File dir, final String file, final In1<String> callback) {
        File target = new File(dir, file);
        if (target.isFile()) {
            final String contents = GFileUtils.readFile(target);
            callback.in(contents);
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

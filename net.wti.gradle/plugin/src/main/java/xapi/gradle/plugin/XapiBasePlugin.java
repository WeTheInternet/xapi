package xapi.gradle.plugin;

import org.gradle.BuildAdapter;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.TaskOutputsInternal;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.*;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.util.GFileUtils;
import xapi.fu.In1;
import xapi.gradle.X_Gradle;
import xapi.gradle.api.ArchiveType;
import xapi.gradle.api.DefaultArchiveType;
import xapi.gradle.config.PlatformConfig;
import xapi.gradle.api.SourceConfig;
import xapi.gradle.java.Java;
import xapi.gradle.publish.Publish;
import xapi.gradle.task.XapiManifest;
import xapi.gradle.tools.Depend;
import xapi.gradle.tools.Ensure;
import xapi.util.X_String;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.File;
import java.util.Date;

import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;
import static xapi.fu.itr.ArrayIterable.iterate;
import static xapi.fu.itr.EmptyIterator.none;

/**
 * The base xapi plugin ensures all the common infrastructure for other plugins is in place.
 * <p>
 * Adds the XapiExtension, `xapi { ... }` to the project,
 * then waits until evaluation is complete to setup additional build tasks.
 * <p>
 * If you use cross-project configuration (allprojects/subprojects),
 * you should probably not apply the basic xapi plugin until the project is evaluated,
 * to give that project time to install a more specific plugin, which might add an extended xapi { } configuration before us.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 1:04 AM.
 */
public class XapiBasePlugin implements Plugin<Project> {

    private final Instantiator instantiator;
    private final ObjectFactory objectFactory;

    @Inject
    public XapiBasePlugin(Instantiator instantiator, ObjectFactory objectFactory) {
        this.instantiator = instantiator;
        this.objectFactory = objectFactory;
    }

    @Override
    public void apply(Project project) {

        project.getPlugins().apply(JavaLibraryPlugin.class);
        project.getPlugins().apply(MavenPublishPlugin.class);
        final ExtensionContainer ext = project.getExtensions();
        final XapiExtension config, existing = ext.findByType(XapiExtension.class);

        ext.create("depend", Depend.class, project);
        X_Gradle.init(project);
        if (existing == null) {
            config = X_Gradle.createConfig(project, instantiator);
        } else {
            config = existing;
        }

        PublishingExtension publishing = (PublishingExtension) project.getExtensions().getByName(PublishingExtension.NAME);

        Publish.addPublishing(project);

        prepareSourceSets(project, config);

        // TODO: set xapi.maven.repo system property / env var for all tests,
        // based on whether there is a known checkout of xapi being used.

        Ensure.projectEvaluated(project, p -> {
            project.getLogger().trace("Project {} initializing config {}", p, config);
            config.initialize(project);
            final TaskProvider<XapiManifest> provider = prepareManifest(project, config);

            project.getGradle().projectsEvaluated(g ->
                provider.configure(man ->
                    man.getInputs().property("paths", man.computeFreshness())
                )
            );
        });

        project.getGradle().projectsEvaluated(gradle -> {
            project.getLogger().trace("Preparing config {}", config);
            config.prepare(project);
            installPomWiring(project, publishing);

        });

        project.getGradle().addBuildListener(new BuildAdapter() {
            @Override
            public void projectsEvaluated(Gradle gradle) {
                // The latest possible callback.
                project.getLogger().trace("Finishing config {}", config);
                config.finish(project);
            }
        });

        project.getPlugins().withType(IdeaPlugin.class, idea -> {
            final IdeaModule module = idea.getModel().getModule();
            final File root = module.getContentRoot();

        });
    }

    private void prepareSourceSets(Project project, XapiExtension config) {

        // Prepare for non-standard source-sets / configurations to be added.
        // We'll want to activate each configuration if there is a) files in src/$type/java,
        // or b) configuration of xapi { types { $type { } } } in build script,

        final ConfigurationContainer configurations = project.getConfigurations();

        config.forArchiveTypes(type -> {
            final String name = toTypeName(type);
            boolean dirExists = new File(project.getProjectDir(), "src/" + type).isDirectory();
            final boolean[] created = {dirExists};
            configurations.register(name, con -> {

                // Bah... BasePlugin uses configurations.all,
                // so we can't actually depend on lazy registration to work here.

                // Instead, we'll hack around it by looking for any dependencies being added...
                con.getDependencies().configureEach(dep -> {
                    if (created[0]) {
                        return;
                    }
                    project.getLogger().quiet("Detected dependency for " + name + " in " + project.getPath());
                    created[0] = true;
                    // Now, prepare for non-standard source-sets / configurations to be added.
                    // We'll want to activate each configuration if there is a) files in src/$type/java,
                    // or b) configuration of xapi { platform { $type { } } } in build script.
                    // Unfortunately, we cannot rely on simply creating the configuration to say "yes, I want this",
                    // as Gradle 5.0 eagerly resolves all configurations.
                    config.defer(()->installSourceSet(project, type, name, con));
                });
            });
            if (dirExists) {
                configurations.getByName(name);
            }
            // wait until the project is fully evaluated,
            // to give the user code a chance to create xapiDev or xapiApi, etc, dependencies.
            // Otherwise, we'd create a vast number of unused configurations and sourcesets
            // (not to mention deploying piles of unused jars, etc.)
            Ensure.projectEvaluated(project, p -> {
                if (created[0]) {
                    project.getLogger().info("Installing xapi flavor {} to {}", type, project.getPath());
                    final Configuration con = configurations.getByName(name);
                    installSourceSet(project, type, name, con);
                }
            });
        });
        final SourceSetContainer sourceSets = Java.sources(project);
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        if (main.getExtensions().findByName(SourceConfig.EXT_NAME) == null) {
            final Configuration con = configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
            //noinspection ConstantConditions,NewObjectEquality
            assert new SourceConfig(project, con, DefaultArchiveType.MAIN, main) != null : "";

            SourceConfig mainConfig = main.getExtensions().create(SourceConfig.EXT_NAME, SourceConfig.class, project, con, DefaultArchiveType.MAIN, main);
            config.sources().add(mainConfig);

            mainConfig.init(
                project.getTasks().named(main.getJarTaskName(), Jar.class),
                project.getTasks().named(main.getCompileJavaTaskName(), JavaCompile.class),
                project.getTasks().named(main.getProcessResourcesTaskName(), ProcessResources.class),
                // TODO: actually get the main publish artifact, or otherwise use a provider to load lazily...
                null
            );
        }

    }

    private void installSourceSet(Project project, String type, String name, Configuration con) {
        final SourceSetContainer sourceSets = Java.sources(project);
        // idempotency is nice (though it would be nicer if we could rely on work avoidance better;
        // both sourceSets and configurations are eagerly resolved when using java / base plugins (respectively).
        if (sourceSets.findByName(name) != null || sourceSets.findByName(type) != null) {
            return;
        }
        XapiExtension ext = XapiExtension.from(project);

        final ConfigurationContainer configurations = project.getConfigurations();

        final SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

        // As of 5.0-rc-4, SourceSetContainer.register() is always immediately processed,
        // as the java and java-base plugins have .all() listeners on sourcesets.
        SourceSet dev = sourceSets.create(name);

        final PlatformConfig isPlatform = ext.findPlatform(type);

        ArchiveType archiveType;
        if (isPlatform == null) {
            archiveType = ArchiveType.coerceArchiveType(type);
        } else {
            archiveType = isPlatform.getType();
        }

        //noinspection ConstantConditions,NewObjectEquality
        assert new SourceConfig(project, con, archiveType, dev) != null : "";
        // ^ here for jump-to-source to show you where we are actually creating it, below
        SourceConfig config = dev.getExtensions().create("xapi", SourceConfig.class, project, con, archiveType, dev);

        ext.sources().add(config);

        if (isPlatform != null) {
            isPlatform.setSources(config);
        }

        // This should be conditional; api and spi would have the reverse relationship.
        final Configuration mainCompileClasspath = configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
        con.extendsFrom(mainCompileClasspath);
        project.getDependencies().add(con.getName(), main.getOutput());

        final Object groovy = dev.getExtensions().findByName("groovy");
        if (groovy != null) {
            ((SourceDirectorySet) groovy).setSrcDirs(none());
        }
        dev.getJava().setSrcDirs(iterate("src/" + type + "/java"));
        dev.getResources().setSrcDirs(iterate("src/" + type + "/resources"));
        Configuration compileClasspath = configurations.getByName(dev.getCompileClasspathConfigurationName());
        compileClasspath.extendsFrom(con);

        Configuration runtimeClasspath = configurations.getByName(dev.getRuntimeClasspathConfigurationName());
        runtimeClasspath.extendsFrom(con);

        Configuration runtime = configurations.getByName(dev.getRuntimeConfigurationName());
        //                    Configuration apiElements = configurations.getByName(dev.getApiElementsConfigurationName());
        //                    Configuration runtimeElements = configurations.getByName(dev.getRuntimeElementsConfigurationName());

        //                    project.getDependencies().getComponents().withModule("real-name:here", ru->
        //                        // also see ru.belongsTo
        //                        ru.withVariant(name, meta->{
        ////                            meta.withCapabilities(cap->
        ////                                cap.addCapability("group", "name", "version"));
        //                        }));

        // Add (optional) transitivity for annotationProcessor paths (inherit main)...
        TaskProvider<Jar> jar = project.getTasks().register(dev.getJarTaskName(), Jar.class,
            j -> {
                j.setDescription("Assembles a jar archive containing the dev classes.");
                j.setGroup(BasePlugin.BUILD_GROUP);

                j.from(dev.getOutput());
                String baseName = j.getBaseName();
                final String prefix = getPrefix();
                if (prefix.isEmpty()) {
                    baseName = baseName + "-" + type;
                } else if (baseName.startsWith(prefix)) {
                    baseName = baseName.replaceFirst(prefix + "(-core|-gwt|-jre|-dev)?", prefix + "-" + type);
                } else {
                    baseName = baseName + "-" + type;
                }
                j.setBaseName(baseName);
            }
        );

        PublishArtifact publish = new LazyPublishArtifact(jar);

        //                    project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(publish);

        TaskProvider<JavaCompile> javaCompile = project.getTasks().named(
            dev.getCompileJavaTaskName(),
            JavaCompile.class
        );
        TaskProvider<ProcessResources> processResources = project.getTasks().named(
            dev.getProcessResourcesTaskName(),
            ProcessResources.class
        );

        //                    addJar(apiElements, publish);
        addJar(runtime, publish);

        addRuntimeVariants(runtime, publish, type, javaCompile, processResources);
        //                    addRuntimeVariants(runtimeElements, publish, javaCompile, processResources);

        //                    project.getComponents().add(objectFactory.newInstance(JavaLibrary.class, project.getConfigurations(), publish));
        // skipped call for JavaLibraryPlatform

        //        project.artifacts.add("xapiDev", publish)
        project.getArtifacts().add("archives", publish);

        project.getPlugins().withType(PublishingPlugin.class).configureEach(plugin -> {
            PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
            publishing.getPublications().create(name, MavenPublication.class,
                pub -> jar.configure(j -> {
                    pub.artifact(j);
                    pub.setArtifactId(j.getBaseName());
                })
            );
        });

        config.init(jar, javaCompile, processResources, publish);


    }

    private void installPomWiring(Project project, PublishingExtension publishing) {
        project.getTasks().withType(GenerateMavenPom.class).all(
            pom -> {
                final MavenPublication main = (MavenPublication) publishing.getPublications().findByName("main");
                if (main != null) {
                    project.getTasks().named(JavaPlugin.JAR_TASK_NAME, t -> {
                        Jar jar = (Jar) t;
                        jar.from(pom.getDestination(), spec -> {
                            spec.into("META-INF/maven/" + main.getGroupId() + "/" + main.getArtifactId());
                            spec.rename(".*", "pom.xml");
                        });
                        // TODO: consider pom.properties as well?
                        // We won't be relying on maven properties for gradle,
                        // so we'll only use them if it makes sense to pick them
                        // back out later in code like X_Maven.
                    });
                }
                if ("true".equals(project.findProperty("dumpPom"))) {
                    pom.doLast(t -> {
                        t.getLogger().quiet(
                            "Pom for {}/{} created at {}",
                            project.getGroup(),
                            project.getName(),
                            pom.getDestination().toURI()
                        );
                        t.getLogger().quiet(GFileUtils.readFileQuietly(pom.getDestination()));
                    });
                }
            });
    }

    protected TaskProvider<XapiManifest> prepareManifest(Project project, XapiExtension config) {

        final TaskContainer tasks = project.getTasks();
        //noinspection NewObjectEquality,ConstantConditions,PointlessBooleanExpression
        assert 1 == 1 || (new XapiManifest() != null); // just here for find-by-source to map constructor to register()
        TaskProvider<XapiManifest> manifest = tasks.register(XapiManifest.MANIFEST_TASK_NAME, XapiManifest.class,
            man -> {
                //            project.getLogger().quiet("Preparing now... " + project.getConfigurations().getByName(JavaPlugin.API_CONFIGURATION_NAME).getState());
                man.setOutputDir(config.outputMeta());

                final FileCollection outputs = man.getOutputs().getFiles();
                project.getDependencies().add(JavaPlugin.API_CONFIGURATION_NAME, outputs);

                // invalidate the task if either processResources or compileJava would be run,
                // as both of them might create output directories that were previously absent.
                man.getOutputs().upToDateWhen(t -> {
                    // Hm... dirty, but...  if we know the processResources and compileJava tasks are up for execution,
                    // then we could pre-emptively mkdirs their output folders here, so the manifest task sees them,
                    // and adds them to the output paths even on clean builds.

                    final Gradle gradle = project.getGradle();
                    boolean uptodate = true;
                    for (Task task : gradle.getTaskGraph().getAllTasks()) {
                        if (task instanceof JavaCompile) {
                            JavaCompile javac = (JavaCompile) task;
                            uptodate &= javac.getState().getUpToDate();
                            // pre-emptively create output directories
                            if (!javac.getSource().isEmpty()) {
                                javac.getDestinationDir().mkdirs();
                            }
                        } else if (task instanceof ProcessResources) {
                            ProcessResources resources = (ProcessResources) task;
                            uptodate &= resources.getState().getUpToDate();
                            // pre-emptively create output directories
                            if (!resources.getSource().isEmpty()) {
                                resources.getDestinationDir().mkdirs();
                            }
                        }
                        // sadly, the getDestinationDir in the above classes is not from any shared type,
                        // so we can't really simplify the above duplication
                    }
                    return uptodate;
                });
                //            project.getLogger().quiet("Preparing now... " + project.getConfigurations().getByName(JavaPlugin.API_CONFIGURATION_NAME).getState());
            }
        );
        // Wire into standard java plugin tasks.
        tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, task -> {
            ProcessResources process = (ProcessResources) task;
            final XapiManifest man = manifest.get();
            process.dependsOn(man);
            process.from(man.getOutputs().getFiles());
            man.finalizedBy(process);
            // ugh... this is kind of backwards.  The manifest task might change,
            // when the processResources or compileJava tasks first create a directory
            // (like on a clean build).
            // Need to reverse this, yet still be able to provide javac w/ manifest information.
            // ...That or more eager output directory creation, to allow the current dependsOn graph
        });
        tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, task -> {
            task.dependsOn(manifest);

            JavaCompile compile = (JavaCompile) task;
            final TaskOutputsInternal outputs = manifest.get().getOutputs();
            compile.setClasspath(
                compile.getClasspath().plus(outputs.getFiles())
            );
        });

        return manifest;
    }

    /**
     * Copied wholesale from {@link JavaPlugin#addJar(Configuration, PublishArtifact)}
     */
    private void addJar(Configuration configuration, PublishArtifact publish) {
        ConfigurationPublications publications = configuration.getOutgoing();

        // Configure an implicit variant
        publications.getArtifacts().add(publish);
        publications.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE);
    }

    /**
     * Copied wholesale from {@link JavaPlugin#addRuntimeVariants(Configuration, PublishArtifact, Provider, Provider)}
     */
    private void addRuntimeVariants(
        Configuration configuration,
        PublishArtifact jarArtifact,
        String type,
        Provider<JavaCompile> javaCompile,
        Provider<ProcessResources> processResources
    ) {
        ConfigurationPublications publications = configuration.getOutgoing();

        // Configure an implicit variant
        publications.getArtifacts().add(jarArtifact);
        publications.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE);

        // Define some additional variants
        NamedDomainObjectContainer<ConfigurationVariant> runtimeVariants = publications.getVariants();
        // not sure we actually need / want the `type+` prefix used here, and for resources, below.
        ConfigurationVariant classesVariant = runtimeVariants.create(type + "classes");
        classesVariant.getAttributes().attribute(
            USAGE_ATTRIBUTE,
            objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_CLASSES)
        );
        classesVariant.artifact(new IntermediateJavaArtifact(ArtifactTypeDefinition.JVM_CLASS_DIRECTORY, javaCompile) {
            @Override
            public File getFile() {
                return javaCompile.get().getDestinationDir();
            }
        });
        ConfigurationVariant resourcesVariant = runtimeVariants.create(type + "resources");
        resourcesVariant.getAttributes().attribute(
            USAGE_ATTRIBUTE,
            objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_RESOURCES)
        );
        resourcesVariant.artifact(new IntermediateJavaArtifact(
            ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY,
            processResources
        ) {
            @Override
            public File getFile() {
                return processResources.get().getDestinationDir();
            }
        });
    }

    protected String getPrefix() {
        return "xapi";
    }

    protected String toTypeName(String type) {
        final String prefix = getPrefix();
        if (X_String.isEmpty(prefix)) {
            return type;
        }
        return prefix + X_String.toTitleCase(type);
    }

    protected Iterable<String> getXapiTypes() {
        return iterate("dev", "api", "spi", "stub", "jre", "gwt", "j2cl", "android");
    }
    /*

    TODO: consider adding new Usage schema...
    private void configureSchema(ProjectInternal project) {
        AttributesSchema attributesSchema = project.getDependencies().getAttributesSchema();
        AttributeMatchingStrategy<Usage> matchingStrategy = attributesSchema.attribute(Usage.USAGE_ATTRIBUTE);
        matchingStrategy.getCompatibilityRules().add(UsageCompatibilityRules.class);
        matchingStrategy.getDisambiguationRules().add(UsageDisambiguationRules.class, new Action<ActionConfiguration>() {
            @Override
            public void execute(ActionConfiguration actionConfiguration) {
                actionConfiguration.params(objectFactory.named(Usage.class, Usage.JAVA_API));
                actionConfiguration.params(objectFactory.named(Usage.class, Usage.JAVA_API_CLASSES));
                actionConfiguration.params(objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_JARS));
                actionConfiguration.params(objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_CLASSES));
                actionConfiguration.params(objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_RESOURCES));
            }
        });

        project.getDependencies().getArtifactTypes().create(ArtifactTypeDefinition.JAR_TYPE).getAttributes().attribute(Usage.USAGE_ATTRIBUTE, objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_JARS));
    }

    */

    /**
     * Borrowed from {@link JavaPlugin}; it's a pain it's not visible...
     * hopefully that isn't a warning sign we're ignoring...
     */
    abstract static class IntermediateJavaArtifact extends AbstractPublishArtifact {
        private final String type;

        IntermediateJavaArtifact(String type, Object task) {
            super(task);
            this.type = type;
        }

        @Override
        public String getName() {
            return getFile().getName();
        }

        @Override
        public String getExtension() {
            return "";
        }

        @Override
        public String getType() {
            return type;
        }

        @Nullable
        @Override
        public String getClassifier() {
            return null;
        }

        @Override
        public Date getDate() {
            return null;
        }
    }
}

package xapi.gradle.plugin;

import net.wti.gradle.PublishXapi;
import net.wti.gradle.internal.impl.IntermediateJavaArtifact;
import net.wti.manifest.ManifestPlugin;
import org.gradle.BuildAdapter;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.configuration.project.ProjectConfigurationActionContainer;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.util.GFileUtils;
import xapi.fu.Lazy;
import xapi.gradle.X_Gradle;
import xapi.gradle.api.ArchiveType;
import xapi.gradle.api.DefaultArchiveType;
import xapi.gradle.api.SourceConfig;
import xapi.gradle.config.PlatformConfig;
import xapi.gradle.java.Java;
import xapi.gradle.publish.Publish;
import xapi.gradle.tools.Depend;
import xapi.gradle.tools.Ensure;
import xapi.string.X_String;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
@Deprecated
public class XapiBasePlugin implements Plugin<Project> {

    private final Instantiator instantiator;
    private final ObjectFactory objectFactory;
    private final ProjectConfigurationActionContainer actions;
    private Lazy<String> prefix;
    private String path;
    private final Map<String, Boolean> created;

    @Inject
    public XapiBasePlugin(Instantiator instantiator, ObjectFactory objectFactory, ProjectConfigurationActionContainer actions) {
        this.instantiator = instantiator;
        this.objectFactory = objectFactory;
        created = new ConcurrentHashMap<>();
        this.actions = actions;
    }

    @Override
    public void apply(Project project) {
        path = project.getPath();
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
        prefix = Lazy.deferred1(config::getPrefix);

        PublishingExtension publishing = (PublishingExtension) project.getExtensions().getByName(PublishingExtension.NAME);

        Publish.addPublishing(project);

        prepareSourceSets(project, config);

        // TODO: set xapi.maven.repo system property / env var for all tests,
        // based on whether there is a known checkout of xapi being used.

        Ensure.projectEvaluated(project, p -> {
            project.getLogger().trace("Project {} initializing config {}", p, config);

            config.initialize(project);

            project.getPlugins().apply(ManifestPlugin.class);

        });

        project.getGradle().projectsEvaluated(gradle -> {
            project.getLogger().trace("Preparing config {}", config);
            config.prepare(project);
            installPomWiring(project, publishing, config);

        });

        project.getGradle().addBuildListener(new BuildAdapter() {
            @Override
            public void projectsEvaluated(Gradle gradle) {
                // The latest possible callback.
                project.getLogger().trace("Finishing config {}", config);
                config.finish(project);
            }
        });
    }

    private void prepareSourceSets(Project project, XapiExtension config) {

        // Prepare for non-standard source-sets / configurations to be added.
        // We'll want to activate each configuration if there is a) files in src/$type/java,
        // or b) configuration of xapi { types { $type { } } } in build script,

        final ConfigurationContainer configurations = project.getConfigurations();

        config.forArchiveTypes(type -> {
            addType(project, config, type);
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

//        final Set<? extends UsageContext> usages = new LinkedHashSet<>();
//
//        project.getComponents().add(new SoftwareComponentInternal() {
//            @Override
//            public Set<? extends UsageContext> getUsages() {
//                return usages;
//            }
//
//            @Override
//            public String getName() {
//                return name;
//            }
//        });

        final SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

        // As of 5.0-rc-4, SourceSetContainer.register() is always immediately processed,
        // as the java and java-base plugins have .all() listeners on sourcesets.
        SourceSet src = sourceSets.create(name);

        final PlatformConfig isPlatform = ext.findPlatform(type);

        ArchiveType archiveType;
        if (isPlatform == null) {
            archiveType = ArchiveType.coerceArchiveType(type);
        } else {
            archiveType = isPlatform.getType();
        }

        //noinspection ConstantConditions,NewObjectEquality
        assert new SourceConfig(project, con, archiveType, src) != null : "";
        // ^ here for jump-to-source to show you where we are actually creating it, below
        SourceConfig config = src.getExtensions().create("xapi", SourceConfig.class, project, con, archiveType, src);

        ext.sources().add(config);

        if (isPlatform != null) {
            isPlatform.setSources(config);
        }

        // This should be conditional; api and spi would have the reverse relationship.
        final Configuration mainCompileClasspath = configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
        for (ArchiveType includes : archiveType.allTypes()) {
            if (includes == DefaultArchiveType.MAIN) {
                project.getDependencies().add(con.getName(), main.getOutput());
                con.extendsFrom(mainCompileClasspath);
            } else {
                final String includeName = includes.prefixedName(getPrefix());
                final Configuration toInclude = configurations.getByName(includeName);
                String srcSetName = includeName;
                if (includes.isSources()) {
                    final ArchiveType srcFor = includes.sourceFor();
                    if (srcFor == null) {
                        continue;
                    }
                    if (srcFor == DefaultArchiveType.MAIN) {
                        srcSetName = SourceSet.MAIN_SOURCE_SET_NAME;
                    } else {
                        srcSetName = srcFor.prefixedName(getPrefix());
                        created.put(srcSetName, true);
                        configurations.getByName(srcSetName);
                    }
                }
                String theName = srcSetName;

                installSourceSet(project, includes.sourceName(), theName, toInclude);
                final SourceSet includeSrc = Java.sources(project).getByName(theName);
                if (includes.isSources()) {
                    for (File srcDir : includeSrc.getAllSource().getSrcDirs()) {
                        try {
                            // consider allowing archive type to filter on filename... later.
                            if (srcDir.isDirectory() && Files.list(Paths.get(srcDir.getAbsolutePath())).findAny().isPresent()) {
                                // Only add directories that exist and have contents.  It will greatly reduce noise.
                                // We may need to loosen this requirement later, in case generated dirs have been cleaned
                                // (or, just make an isGenerated check for this conditional).
                                project.getDependencies().add(con.getName(), project.files(srcDir));
                            }
                        } catch (IOException e) {
                            project.getLogger().warn("Unexpected IOE checking {}", srcDir, e);
                        }
                    }

                } else {
                    project.getDependencies().add(con.getName(), includeSrc.getOutput());
                    con.extendsFrom(toInclude);
                }
            }
        }

        if (DefaultArchiveType.MAIN.includes(archiveType)) {
            mainCompileClasspath.extendsFrom(con);
            project.getDependencies().add("implementation", src.getOutput());
        }

        final Object groovy = src.getExtensions().findByName("groovy");
        if (groovy != null) {
            ((SourceDirectorySet) groovy).setSrcDirs(none());
        }
        src.getJava().setSrcDirs(iterate("src/" + type + "/java"));
        src.getResources().setSrcDirs(iterate("src/" + type + "/resources"));
        Configuration compileClasspath = configurations.getByName(src.getCompileClasspathConfigurationName());
        compileClasspath.extendsFrom(con);

        Configuration runtimeClasspath = configurations.getByName(src.getRuntimeClasspathConfigurationName());
        runtimeClasspath.extendsFrom(con);

        Configuration runtime = configurations.getByName(src.getRuntimeConfigurationName());
        //                    Configuration apiElements = configurations.getByName(dev.getApiElementsConfigurationName());
        //                    Configuration runtimeElements = configurations.getByName(dev.getRuntimeElementsConfigurationName());

        //                    project.getDependencies().getComponents().withModule("real-name:here", ru->
        //                        // also see ru.belongsTo
        //                        ru.withVariant(name, meta->{
        ////                            meta.withCapabilities(cap->
        ////                                cap.addCapability("group", "name", "version"));
        //                        }));


        // Add (optional) transitivity for annotationProcessor paths (inherit main)...
        TaskProvider<Jar> jar = project.getTasks().register(src.getJarTaskName(), Jar.class,
            j -> {
                j.setDescription("Assembles a jar archive containing the " + name + " classes.");
                j.setGroup(LifecycleBasePlugin.BUILD_GROUP);

                j.from(src.getOutput());
                String baseName = j.getArchiveBaseName().get();
                final String prefix = getPrefix();
                if (prefix.isEmpty() || baseName.startsWith(prefix)) {
                    baseName = baseName + "-" + type;
//                } else if (baseName.startsWith(prefix)) {
//                    baseName = baseName.replaceFirst(prefix + "(-core|-gwt|-jre|-dev)?", prefix + "-" + type);
                } else {
                    baseName = baseName + "-" + type;
                }
                j.getArchiveBaseName().set(baseName);
//                // Tell gradle that this jar can be used if someone addresses it's final published coordinates
//                String coords = project.getGroup() + ":" +
//                    baseName + ":" +
//                    project.getVersion();
//                con.getOutgoing().capability(coords);

//                j.setAppendix(type);
                project.getLogger().debug("Setting up artifact {}", j.getBaseName());
            }
        );

        PublishArtifact publish = new LazyPublishArtifact(jar);

        //                    project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(publish);

        TaskProvider<JavaCompile> javaCompile = project.getTasks().named(
            src.getCompileJavaTaskName(),
            JavaCompile.class
        );
        TaskProvider<ProcessResources> processResources = project.getTasks().named(
            src.getProcessResourcesTaskName(),
            ProcessResources.class
        );

        addRuntimeVariants(runtime, publish, type, javaCompile, processResources);
        //                    addRuntimeVariants(runtimeElements, publish, javaCompile, processResources);

        //                    project.getComponents().add(objectFactory.newInstance(JavaLibrary.class, project.getConfigurations(), publish));
        // skipped call for JavaLibraryPlatform

        //        project.artifacts.add("xapiDev", publish)
        project.getArtifacts().add("archives", publish);
        project.getArtifacts().add(con.getName(), publish);

        project.getPlugins().withType(PublishingPlugin.class).configureEach(plugin -> {
            PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
            // Instead of this, try adding variants to the main publication instead.
            publishing.getPublications().create(name, MavenPublication.class,
                pub -> jar.configure(j -> {
                    // We need to be adding an entire SoftwareComponent, instead of just an artifact.
                    pub.artifact(j);
                    pub.setArtifactId(j.getBaseName());
                })
            );
            // TODO: make this way less hacky / possible to change as gradle task names evolve.
            String publishTask = "publish" + X_String.toTitleCase(name) + "PublicationToXapiLocalRepository";
            final Task xapiPublish = PublishXapi.getPublishXapiTask(project);
            project.getTasks().named(publishTask, PublishToMavenRepository.class, xapiPublish::dependsOn);
        });

        config.init(jar, javaCompile, processResources, publish);


    }

    private void installPomWiring(
        Project project,
        PublishingExtension publishing,
        XapiExtension config
    ) {
        project.getTasks().withType(GenerateMavenPom.class).all(
            pom -> {
                // hm...  should be finding the publication from the task, which requires nasty taskname dissection...
                final MavenPublication main = config.getMainPublication();
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
        ConfigurationVariant classesVariant = runtimeVariants.create("classes");
        classesVariant.getAttributes().attribute(
            USAGE_ATTRIBUTE,
            getClassUsage()
        );

        // Must have a Usage.JAVA_API usage case, for compile-scoping
        classesVariant.artifact(new IntermediateJavaArtifact(ArtifactTypeDefinition.JVM_CLASS_DIRECTORY, javaCompile) {
            @Override
            public File getFile() {
                return javaCompile.get().getDestinationDir();
            }
        });

        ConfigurationVariant resourcesVariant = runtimeVariants.create("resources");
        resourcesVariant.getAttributes().attribute(
            USAGE_ATTRIBUTE,
            getResourceUsage()
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

    private Usage getResourceUsage() {
        return objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_RESOURCES);
    }

    private Usage getClassUsage() {
        return objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_CLASSES);
    }

    protected String getPrefix() {
        return prefix.out1();
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

    public void addTypes(Project project, String ... types) {
        final XapiExtension ext = (XapiExtension) project.getExtensions().getByName(XapiExtension.EXT_NAME);
//        ext.onInit(()->{
            for (String type : types) {
                addType(project, ext, type);
            }
//        });

    }
    private void addType(Project project, XapiExtension config, String type) {
        final String name = ArchiveType.toTypeName(getPrefix(), type);
        final ConfigurationContainer configurations = project.getConfigurations();
        if (configurations.findByName(name) != null) {
            project.getLogger().info("Configuration {} already exists; not creating Xapi config.", name);
            return;
        }
        boolean dirExists = new File(project.getProjectDir(), "src/" + type).isDirectory();
        this.created.put(name, dirExists);
        configurations.register(name, con -> {

            // Bah... BasePlugin uses configurations.all,
            // so we can't actually depend on lazy registration to work here.

            // Instead, we'll hack around it by looking for any dependencies being added...
            con.getDependencies().configureEach(dep -> {
                // Should use incoming.beforeResolve instead... but, this is deprecated anyway, so will delete instead (later)
                if (Boolean.TRUE.equals(created.get(name))) {
                    return;
                }
                project.getLogger().quiet("Detected dependency for " + name + " in " + project.getPath());
                created.put(name, true);
                // Now, prepare for non-standard source-sets / configurations to be added.
                // We'll want to activate each configuration if there is a) files in src/$type/java,
                // or b) configuration of xapi { platform { $type { } } } in build script.
                // Unfortunately, we cannot rely on simply creating the configuration to say "yes, I want this",
                // as Gradle 5.0 eagerly resolves all configurations.
                config.defer(()->
                    installSourceSet(project, type, name, con)
                )
                ;
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
            if (Boolean.TRUE.equals(created.get(name))) {
                project.getLogger().info("Installing xapi flavor {} to {}", type, project.getPath());
                final Configuration con = configurations.getByName(name);

                installSourceSet(project, type, name, con);
            }
        });
    }
}

package net.wti.gradle.system.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.impl.IntermediateJavaArtifact;
import org.gradle.api.*;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry;
import org.gradle.internal.component.external.model.JavaEcosystemVariantDerivationStrategy;
import org.gradle.internal.component.external.model.VariantDerivationStrategy;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.Method;

import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;
import static org.gradle.api.plugins.JavaPlugin.*;

/**
 * This is an internally-applied-only plugin,
 * which is meant for use when the user has not applied or requested the regular java plugin.
 *
 * There is a little bit of functionality that we need, and a bunch that we don't.
 *
 * In particular, there are many configurations and wiring that just get in our way,
 * so we'd rather use this shim than try to interface directly with the java plugin itself.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2/18/19 @ 12:39 PM.
 */
@SuppressWarnings("UnstableApiUsage")
public class XapiJavaPlugin implements Plugin<ProjectInternal> {

    private final ObjectFactory objectFactory;
    private final TaskDependencyFactory taskFactory;


    @Inject
    public XapiJavaPlugin(final ObjectFactory objectFactory, final TaskDependencyFactory taskFactory) {
        this.objectFactory = objectFactory;
        this.taskFactory = taskFactory;
    }

    @Override
    public void apply(ProjectInternal project) {
        project.getPlugins().apply(JavaBasePlugin.class);
        project.getPlugins().withType(JavaPlugin.class).all(illegal-> {
            throw new GradleException("Do not apply java plugin and XapiJavaPlugin at the same time.\n" +
                "Really, just don't apply XapiJavaPlugin, it will be applied for you through XapiBasePlugin.");
                // There is not a registered plugin id for this class for this exact reason.
                // It's where we're putting our "running without java plugin" shims (that are mutually exclusive w/ java plugin).
        });
        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        BuildOutputCleanupRegistry buildOutputCleanupRegistry = project.getServices().get(BuildOutputCleanupRegistry.class);

        configureSourceSets(javaConvention, buildOutputCleanupRegistry);
        configureConfigurations(project, javaConvention);

        configureJavaDoc(javaConvention);
        configureTest(project, javaConvention);
//        configureArchivesAndComponent(project, javaConvention);
        configureBuild(project);

        final ProjectView view = ProjectView.fromProject(project);
        // we will probably want to create our own strategy and abandon this one...
        VariantDerivationStrategy strat;
        try {
            strat = new JavaEcosystemVariantDerivationStrategy();
        } catch (IllegalAccessError ignored) {
            try {
                final Method method = JavaEcosystemVariantDerivationStrategy.class.getMethod("getInstance");
                final Object result = method.invoke(null);
                strat = (VariantDerivationStrategy) result;
            } catch (Exception e) {
                throw new RuntimeException("Unable to get a JavaEcosystemVariantDerivationStrategy", e);
            }
        }
        view.getComponentMetadata().setVariantDerivationStrategy(strat);
    }

    private void configureSourceSets(JavaPluginConvention pluginConvention, final BuildOutputCleanupRegistry buildOutputCleanupRegistry) {
        Project project = pluginConvention.getProject();

        SourceSet main = pluginConvention.getSourceSets().maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME);

        SourceSet test = pluginConvention.getSourceSets().maybeCreate(SourceSet.TEST_SOURCE_SET_NAME);
        test.setCompileClasspath(project.getLayout().configurableFiles(main.getOutput(), project.getConfigurations().getByName(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME)));
        test.setRuntimeClasspath(project.getLayout().configurableFiles(test.getOutput(), main.getOutput(), project.getConfigurations().getByName(TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME)));

        // Register the project's source set output directories
        pluginConvention.getSourceSets().all(sourceSet -> buildOutputCleanupRegistry.registerOutputs(sourceSet.getOutput()));
    }

    private void configureConfigurations(ProjectInternal project, JavaPluginConvention javaConvention) {
        ConfigurationContainer configurations = project.getConfigurations();

        Configuration compileConfiguration = configurations.getByName(COMPILE_CONFIGURATION_NAME);
        Configuration implementationConfiguration = configurations.getByName(IMPLEMENTATION_CONFIGURATION_NAME);
        Configuration runtimeConfiguration = configurations.getByName(RUNTIME_CONFIGURATION_NAME);
        Configuration runtimeOnlyConfiguration = configurations.getByName(RUNTIME_ONLY_CONFIGURATION_NAME);
        Configuration compileTestsConfiguration = configurations.getByName(TEST_COMPILE_CONFIGURATION_NAME);
        Configuration testImplementationConfiguration = configurations.getByName(TEST_IMPLEMENTATION_CONFIGURATION_NAME);
        Configuration testRuntimeConfiguration = configurations.getByName(TEST_RUNTIME_CONFIGURATION_NAME);
        Configuration testRuntimeOnlyConfiguration = configurations.getByName(TEST_RUNTIME_ONLY_CONFIGURATION_NAME);
        Configuration testCompileClasspathConfiguration = configurations.getByName(TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME);
        Configuration testRuntimeClasspathConfiguration = configurations.getByName(TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME);

        compileTestsConfiguration.extendsFrom(compileConfiguration);
        testImplementationConfiguration.extendsFrom(implementationConfiguration);
        testRuntimeConfiguration.extendsFrom(runtimeConfiguration);
        // blech... this causes testRuntime to try to be resolved as a configuration-with-capability,
        // mostly because we've been lazy about isolating runtime classpaths; we should emulate the same
        // structure as compile classpaths, where we do not add any outgoing settings until _after_
        // the "dependency bucket" configurations, like runtimeConfiguration
        testRuntimeConfiguration.setCanBeConsumed(false);
        testRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration);
        testCompileClasspathConfiguration.setCanBeConsumed(false);
        testRuntimeClasspathConfiguration.setCanBeConsumed(false);

    }

    private void configureJavaDoc(final JavaPluginConvention pluginConvention) {
        Project project = pluginConvention.getProject();
        project.getTasks().register(JAVADOC_TASK_NAME, Javadoc.class, new Action<Javadoc>() {
            @Override
            public void execute(Javadoc javadoc) {
                final SourceSet mainSourceSet = pluginConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                javadoc.setDescription("Generates Javadoc API documentation for the main source code.");
                javadoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP);
                javadoc.setClasspath(mainSourceSet.getOutput().plus(mainSourceSet.getCompileClasspath()));
                javadoc.setSource(mainSourceSet.getAllJava());
            }
        });
    }

    private void configureArchivesAndComponent(Project project, final JavaPluginConvention pluginConvention) {
        TaskProvider<Jar> jar = project.getTasks().register(JAR_TASK_NAME, Jar.class, new Action<Jar>() {
            @Override
            public void execute(Jar jar) {
                jar.setDescription("Assembles a jar archive containing the main classes.");
                jar.setGroup(BasePlugin.BUILD_GROUP);
                jar.from(pluginConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput());
            }
        });
        // TODO: Allow this to be added lazily
        PublishArtifact jarArtifact = new LazyPublishArtifact(jar);
//        Configuration apiElementConfiguration = project.getConfigurations().getByName(API_ELEMENTS_CONFIGURATION_NAME);
        Configuration runtimeConfiguration = project.getConfigurations().getByName(RUNTIME_CONFIGURATION_NAME);
        Configuration runtimeElementsConfiguration = project.getConfigurations().getByName(RUNTIME_ELEMENTS_CONFIGURATION_NAME);

        project.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(jarArtifact);

        Provider<JavaCompile> javaCompile = project.getTasks().named(COMPILE_JAVA_TASK_NAME, JavaCompile.class);
        Provider<ProcessResources> processResources = project.getTasks().named(PROCESS_RESOURCES_TASK_NAME, ProcessResources.class);

//        addJar(apiElementConfiguration, jarArtifact);
        addJar(runtimeConfiguration, jarArtifact);
        addRuntimeVariants(runtimeElementsConfiguration, jarArtifact, javaCompile, processResources);

    }

    private void addJar(Configuration configuration, PublishArtifact jarArtifact) {
        ConfigurationPublications publications = configuration.getOutgoing();

        // Configure an implicit variant
        publications.getArtifacts().add(jarArtifact);
        publications.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE);
    }

    private void addRuntimeVariants(Configuration configuration, PublishArtifact jarArtifact, final Provider<JavaCompile> javaCompile, final Provider<ProcessResources> processResources) {
        ConfigurationPublications publications = configuration.getOutgoing();

        // Configure an implicit variant
        publications.getArtifacts().add(jarArtifact);
        publications.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE);

        // Define some additional variants
        NamedDomainObjectContainer<ConfigurationVariant> runtimeVariants = publications.getVariants();
        ConfigurationVariant classesVariant = runtimeVariants.create("classes");
        classesVariant.getAttributes().attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_CLASSES));
        classesVariant.artifact(new IntermediateJavaArtifact(taskFactory, ArtifactTypeDefinition.JVM_CLASS_DIRECTORY, javaCompile) {
            @Override
            public File getFile() {
                return javaCompile.get().getDestinationDir();
            }
        });
        ConfigurationVariant resourcesVariant = runtimeVariants.create("resources");
        resourcesVariant.getAttributes().attribute(USAGE_ATTRIBUTE, objectFactory.named(Usage.class, Usage.JAVA_RUNTIME_RESOURCES));
        resourcesVariant.artifact(new IntermediateJavaArtifact(taskFactory, ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY, processResources) {
            @Override
            public File getFile() {
                return processResources.get().getDestinationDir();
            }
        });
    }

    private void configureBuild(Project project) {
        project.getTasks().named(JavaBasePlugin.BUILD_NEEDED_TASK_NAME,
            task -> addDependsOnTaskInOtherProjects(task, true,
                JavaBasePlugin.BUILD_NEEDED_TASK_NAME, JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        );
        project.getTasks().named(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME,
            task -> addDependsOnTaskInOtherProjects(task, false,
                JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME, JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        );
    }

    private void configureTest(final Project project, final JavaPluginConvention pluginConvention) {
        project.getTasks().withType(Test.class).configureEach(test -> {
            test.getConventionMapping().map("testClassesDirs",
                () -> pluginConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getOutput().getClassesDirs()
            );
            test.getConventionMapping().map("classpath",
                () -> pluginConvention.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME).getRuntimeClasspath()
            );
        });

        final Provider<Test> test = project.getTasks().register(JavaPlugin.TEST_TASK_NAME, Test.class, test1 -> {
            test1.setDescription("Runs the unit tests.");
            test1.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
        });
        project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME, task -> task.dependsOn(test));
    }

    private void addDependsOnTaskInOtherProjects(final Task task, boolean useDependedOn, String otherProjectTaskName,
                                                 String configurationName) {
        Project project = task.getProject();
        final Configuration configuration = project.getConfigurations().getByName(configurationName);
        task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, otherProjectTaskName));
    }
}

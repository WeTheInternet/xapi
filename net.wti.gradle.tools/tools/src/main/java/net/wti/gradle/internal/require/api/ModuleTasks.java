package net.wti.gradle.internal.require.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.impl.DefaultArchiveGraph;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.SourceMeta;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.util.GUtil;

/**
 * A centralized collection of module-wide task objects.
 *
 * This is factored out into a type of its own,
 * so {@link ArchiveGraph} can make use of this in default interface methods,
 * to remove noise from ArchiveGraph, and to make it clearly visible
 * what relevant tasks are / can be created for a given module.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/21/19 @ 1:05 AM.
 */
public class ModuleTasks {

    private final DefaultArchiveGraph source;

    private TaskProvider<Jar> jarTask;
    private TaskProvider<Jar> sourceJarTask;
    private TaskProvider<JavaCompile> javaCompileTask;
    private TaskProvider<ProcessResources> processResourcesTask;
    private TaskProvider<Test> testTask;

    public ModuleTasks(DefaultArchiveGraph source) {
        this.source = source;
    }

    private SourceMeta meta() {
        return source.getSource();
    }

    private ProjectView view() {
        return source.getView();
    }

    public TaskProvider<ProcessResources> getProcessResourcesTask() {
        if (processResourcesTask == null) {
            final String name = meta().getSrc().getProcessResourcesTaskName();
            processResourcesTask = view().getTasks().named(name, ProcessResources.class);
        }
        return processResourcesTask;
    }
    public TaskProvider<JavaCompile> getJavacTask() {
        if (javaCompileTask == null) {
            final String name = meta().getSrc().getCompileJavaTaskName();
            javaCompileTask = view().getTasks().named(name, JavaCompile.class);

////            final Configuration config = source.configExportedApi();
//            final Configuration config = source.configAssembled();
//            final ConfigurationPublications publications = config.getOutgoing();
//            final LazyPublishArtifact artifact = new LazyPublishArtifact(view().lazyProvider(()->
//                javaCompileTask.get().getDestinationDir()), view().getVersion()) {
//                @Override
//                public TaskDependency getBuildDependencies() {
//                    return new AbstractTaskDependency() {
//                        @Override
//                        public void visitDependencies(TaskDependencyResolveContext context) {
//                            context.add(javaCompileTask.get());
//                        }
//                    };
//                }
//            };
//            // Bleh...  cannot treat directories as published artifacts w.r.t. gradle metadata.
//            // Which makes sense... any directories would only exist locally anyway.
//            // Instead of this, we are going to remove this artifact from the publication for now.
//            // We'll leave it in the variant, since that will effect runtime dependency resolution.
////            publications.artifact(artifact);
//
//            // Need to move the variants themselves into a lazily-initialized field as well,
//            // so we can easily lookup accessors and do one-time setup on them.
//            final ConfigurationVariant runtimeVariant = publications.getVariants().maybeCreate(
//                "classes");
//            runtimeVariant.artifact(artifact);
//            source.withAttributes(runtimeVariant.getAttributes());
//            runtimeVariant.getAttributes().attribute(
//                Usage.USAGE_ATTRIBUTE, view().getProjectGraph().usage(Usage.JAVA_API_CLASSES)
//            );

        }
        return javaCompileTask;
    }
    public TaskProvider<Jar> getJarTask() {
        if (jarTask == null) {
            final TaskContainer tasks = view().getTasks();
            final String name = meta().getSrc().getJarTaskName();
            if (tasks.findByName(name) != null || ("jar".equals(name) && view().isJavaCompatibility())) {
                // main jar needs special casing.
                jarTask = tasks.named(name, Jar.class);
            } else {
                // everything else, we create on-demand.
                jarTask = tasks.register(name, Jar.class, jar->{
                    jar.from(getJavacTask().get().getOutputs());
                    jar.from(getProcessResourcesTask().get().getOutputs());

                    jar.setGroup(BasePlugin.BUILD_GROUP);
                    jar.setDescription("Assemble jar of " + source.getSrcName() + " classes.");
                });
            }
            tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).configure(assemble ->
                assemble.dependsOn(jarTask)
            );
            jarTask.configure(jar->{
                jar.setGroup(source.getGroup());
                jar.getArchiveBaseName().set(
                    view().lazyProvider(source::getModuleName)
                );
            });

            return jarTask;


        }
        return jarTask;
    }

    public TaskProvider<Jar> getSourceJarTask() {
        if (sourceJarTask == null) {
            SourceMeta meta = meta();
            final SourceSet src = meta.getSrc();
            final String name = src.getTaskName(null, "SourceJar");
            sourceJarTask = view().getTasks().register(name, Jar.class, jar->{
                if (source.srcExists()) {
                    jar.from(src.getAllSource());
                    jar.getExtensions().add(SourceMeta.EXT_NAME, meta);
                }

                jar.setGroup(source.getGroup());
                jar.setDescription("Assemble jar of " + source.getSrcName() + " sources.");

                final XapiSchema schema = view().getSchema();
                if (schema.getArchives().isWithCoordinate()) {
                    assert !schema.getArchives().isWithClassifier() : "Archive container cannot use both coordinate and classifier: " + schema.getArchives();
                    jar.getArchiveAppendix().set("sources");
                } else {
                    jar.getArchiveClassifier().set("sources");
                    view().getLogger().quiet("Using sources classifier for {}", name);
                }

            });

            return sourceJarTask;
        }
        return sourceJarTask;
    }

    public TaskProvider<Test> getTestTask() {
        if (testTask == null) {
            String name = "test" + GUtil.toCamelCase(source.getConfigName());
            if ("testTest".equals(name)) {
                // main jar needs special casing.
                name = "test";
                testTask = view().getTasks().named(name, Test.class);
            } else {
                // everything else, we create on-demand.
                testTask = view().getTasks().register(name, Test.class, test->{
                    test.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
                    test.setDescription("Runs unit tests for " + source.getCapabilityCore());
                });
            }

            testTask.configure(test->{
                test.setTestClassesDirs(
                    meta().getSrc().getOutput().getClassesDirs()
                );

                final FileCollection classpath = meta().getSrc().getRuntimeClasspath();
                test.setClasspath(classpath);
//                view().getLogger().quiet("Test classpath for {} is:\n{}",
//                    view().getPath(), classpath.getAsPath());
            });

            return testTask;


        }
        return testTask;

    }
}

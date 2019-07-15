package net.wti.manifest;

import net.wti.gradle.internal.api.ProjectView;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.TaskOutputsInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.TaskState;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import xapi.gradle.task.XapiManifest;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 15/07/19 @ 1:44 AM.
 */
public class ManifestPlugin implements Plugin<Project> {

    @Override
    public void apply(Project p) {
        ProjectView view = ProjectView.fromProject(p);
        view.getSchema().whenReady(schema ->
            // create manifest job
            prepareManifest(view)
        );
    }


    protected TaskProvider<XapiManifest> prepareManifest(ProjectView view) {

        final TaskContainer tasks = view.getTasks();
        TaskProvider<XapiManifest> manifest = tasks.register(XapiManifest.MANIFEST_TASK_NAME, XapiManifest.class,
            man -> {

                final FileCollection outputs = man.getOutputs().getFiles();
                view.getDependencies().add(JavaPlugin.API_CONFIGURATION_NAME, outputs);

                // invalidate the task if either processResources or compileJava would be run,
                // as both of them might create output directories that were previously absent.
                man.getOutputs().upToDateWhen(t -> {
                    // Hm... dirty, but...  if we know the processResources and compileJava tasks are up for execution,
                    // then we could pre-emptively mkdirs their output folders here, so the manifest task sees them,
                    // and adds them to the output paths even on clean builds.

                    final Gradle gradle = view.getGradle();
                    boolean uptodate = true;
                    for (Task task : gradle.getTaskGraph().getAllTasks()) {
                        final TaskState state = task.getState();
                        if (task instanceof JavaCompile) {
                            JavaCompile javac = (JavaCompile) task;
                            uptodate &= state.getUpToDate() || state.getNoSource() || state.getSkipped();
                            // pre-emptively create output directories
                            if (!javac.getSource().isEmpty()) {
                                javac.getDestinationDir().mkdirs();
                            }
                        } else if (task instanceof ProcessResources) {
                            ProcessResources resources = (ProcessResources) task;
                            uptodate &= state.getUpToDate() || state.getNoSource() || state.getSkipped();
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

                man.getInputs().property("paths", man.computeFreshness());
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
}

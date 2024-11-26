package net.wti.gradle.system.tools;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.testing.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * TestIsolator:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 03/05/2021 @ 1:13 a.m.
 */
public class TestIsolator extends DefaultTask {

    public static final String EXT_ROOT_ISOLATOR = "xapiRootIsolator";
    private final TestIsolator rootIsolator;
    private final Set<String> isolatedTests;

    public TestIsolator() {
        final Project project = getProject();
        final Project root = project.getRootProject();
        isolatedTests = new HashSet<>();
        if (project == root) {
            rootIsolator = this;
            initializeRootTask(this);
        } else {
            rootIsolator = root.getTasks().maybeCreate(EXT_ROOT_ISOLATOR, TestIsolator.class);
            // All other isolator tasks depend on the root task, which depends on all other not-isolated Test tasks.
            dependsOn(rootIsolator);
        }
    }

    private void initializeRootTask(final TestIsolator rootTask) {
        rootTask.getProject().getGradle().projectsEvaluated(g->{
            rootTask.getProject().allprojects(otherProj -> {
                otherProj.getTasks().withType(Test.class).configureEach(test -> {
                    // used to use test.whenSelected here... can't do that on mainline gradle versions
                    if (! isolatedTests.contains(test.getPath())) {
                        rootTask.mustRunAfter(test.getPath());
                    }
                });
            });
        });
        rootTask.setGroup("Verification");
        rootTask.setDescription("The root TestIsolator instance mustRunAfter all Tests which have not had TestIsolator.addTask() called");
    }

    @TaskAction
    public void doNothing() {
        // we need a task action, but this task is a lifecycle task, so we need an empty method to annotate
    }

    public void addAll() {
        getProject()
                .getTasks()
                .withType(Test.class)
                .configureEach(this::addTest);
    }

    public void addTest(Test t) {
        rootIsolator.isolatedTests.add(t.getPath());
        // The test depends on this isolator, which itself depends on the root isolator which mustRunAfter:
        // "all Test tasks which are not isolated".
        t.dependsOn(this);
    }
}

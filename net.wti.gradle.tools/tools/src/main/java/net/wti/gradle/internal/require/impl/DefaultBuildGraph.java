package net.wti.gradle.internal.require.impl;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.system.api.RealizableNamedObjectContainer;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.BuildAdapter;
import org.gradle.BuildResult;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.internal.build.IncludedBuildState;

import javax.inject.Inject;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/1/19 @ 12:29 AM.
 */
public class DefaultBuildGraph extends AbstractBuildGraphNode<ProjectGraph> implements BuildGraph {

    private final GradleService service;
    private final ProjectView project;

    @Inject
    public DefaultBuildGraph(GradleService service, ProjectView project) {
        super(ProjectGraph.class, project);
        this.service = service;
        this.project = project;

        project.whenReady(ready-> {
            drainTasks(ReadyState.BEFORE_CREATED);
            drainTasks(ReadyState.CREATED);
            drainTasks(ReadyState.AFTER_CREATED);
        });

        project.getGradle().projectsEvaluated(done->finalizeGraph());

        project.getGradle().addBuildListener(new BuildAdapter(){
            @Override
            public void projectsEvaluated(Gradle gradle) {
                drainTasks(ReadyState.BEFORE_FINISHED);
                drainTasks(ReadyState.FINISHED);
                drainTasks(ReadyState.AFTER_FINISHED);
            }

            @Override
            public void buildFinished(BuildResult result) {
                drainTasks(Integer.MAX_VALUE);
            }
        });
    }

    public void finalizeGraph() {
        drainTasks(
            ReadyState.EXECUTE - 1 // == Short.MAX_VALUE
        );
    }

    public RealizableNamedObjectContainer<ProjectGraph> getProjects() {
        return getItems();
    }

    @Override
    protected ProjectGraph createItem(String name) {
        name = toPath(name);
        final ProjectView proj = project.findProject(name);
        if (proj == null) {
            throw new IllegalArgumentException("No projectView named " + name + "; " +
                "accessed by " + project.getDebugPath() );
        }
        return new DefaultProjectGraph(DefaultBuildGraph.this, proj);
    }

    private String toPath(String name) {
        return name.startsWith(":") ? name : ":" + name;
    }

    @Override
    public NamedDomainObjectProvider<ProjectGraph> project(Object path) {
        final String projectPath = projectPath(path);

        // this is a really ugly assert, but we really don't want to run any of this code in production.
        // first, check if this path exists in the root project
        assert project.getGradle().getRootProject().findProject(projectPath) != null ||
            // next, check if this path exists in an included build.
            // you really shouldn't even get here...
            project.getGradle().getIncludedBuilds().stream().anyMatch(
                // the Law of Demeter weeps...
                build -> ((IncludedBuildState)build).getConfiguredBuild().getRootProject().findProject(projectPath) != null
            );
        return getOrRegister(projectPath);
    }

    @Override
    public boolean hasProject(Object path) {
        final String projectPath = projectPath(path);
        return project.getGradle().getRootProject().findProject(projectPath) != null;
    }

    private String projectPath(Object path) {
        String p = GradleCoerce.unwrapStringOr(path, ":");
        return p.startsWith(":") ? p : ":" + p;
    }

    @Override
    public ProjectView rootProject() {
        // Perhaps also have a method for the schema root;
        // For now, the build graph is global across all possible schemas,
        // and it's likely that we'll want to keep it that way,
        // in case we find it useful to opt-out of a root schema...
        return service.getView();
    }

    @Override
    public Set<ProjectGraph> realizedProjects() {
        return realizedItems();
    }

    @Override
    public Set<String> registeredProjects() {
        return registeredItems();
    }

}

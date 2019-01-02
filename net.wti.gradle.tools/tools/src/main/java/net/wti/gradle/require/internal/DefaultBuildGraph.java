package net.wti.gradle.require.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.require.internal.BuildGraph.ProjectGraph;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.NamedDomainObjectProvider;

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
        super(ProjectGraph.class, project.getInstantiator());
        this.service = service;
        this.project = project;
    }

    @Override
    protected ProjectGraph createItem(String name) {
        return new DefaultProjectGraph(DefaultBuildGraph.this, project);
    }

    @Override
    public NamedDomainObjectProvider<ProjectGraph> project(Object path) {
        String p = GradleCoerce.unwrapStringOr(path, ":");
        return getOrRegister(p);
    }

    @Override
    public ProjectView getProject() {
        return project;
    }

    @Override
    public ProjectView rootProject() {
        return ProjectView.fromProject(service.getProject());
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

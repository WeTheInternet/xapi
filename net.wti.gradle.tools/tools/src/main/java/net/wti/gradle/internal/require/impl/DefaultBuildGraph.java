package net.wti.gradle.internal.require.impl;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
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
        super(ProjectGraph.class, project);
        this.service = service;
        this.project = project;
    }

    @Override
    protected ProjectGraph createItem(String name) {
        name = toPath(name);
        return new DefaultProjectGraph(DefaultBuildGraph.this, project.findProject(name));
    }

    private String toPath(String name) {
        return name.startsWith(":") ? name : ":" + name;
    }

    @Override
    public NamedDomainObjectProvider<ProjectGraph> project(Object path) {
        String p = GradleCoerce.unwrapStringOr(path, ":");
        return getOrRegister(p.startsWith(":") ? p : ":" + p);
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

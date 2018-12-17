package net.wti.gradle.system.spi;

import net.wti.gradle.system.service.GradleService;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import xapi.gradle.java.Java;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/15/18 @ 5:07 AM.
 */
public class DefaultGradleService implements GradleService {

    private final Project project;

    public DefaultGradleService(Project project) {
        this.project = project;
        if (project == project.getRootProject()) {
            configureWrapper(project);
        }
        project.getTasks().configureEach(this::taskInstall);
        project.getPlugins().withId("java-base", plug->
            Java.sources(project).configureEach(this::sourceInstall));
    }

    protected void sourceInstall(SourceSet source) {
        GradleService.buildOnce(source, GradleService.EXT_NAME, t->this);
    }

    protected void taskInstall(Task task) {
        GradleService.buildOnce(task, GradleService.EXT_NAME, t->this);
    }

    @Override
    public Project getProject() {
        return project;
    }
}

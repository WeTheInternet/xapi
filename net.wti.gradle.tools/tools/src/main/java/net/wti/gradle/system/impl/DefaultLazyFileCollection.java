package net.wti.gradle.system.impl;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.system.api.LazyFileCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.file.AbstractFileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskDependency;

import java.io.File;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/19/19 @ 12:02 AM.
 */
public class DefaultLazyFileCollection extends AbstractFileCollection implements LazyFileCollection {

    private final String name;
    private final Provider<Set<File>> files;
    private final Provider<TaskDependency> taskDependency;

    public DefaultLazyFileCollection(ProjectView view, String name, Provider<Set<File>> files, Provider<TaskDependency> taskDependency) {
        this.name = name;
        this.files = view.lazyProvider(files);
        this.taskDependency = view.lazyProvider(taskDependency);
    }

    public DefaultLazyFileCollection(ProjectView view, Configuration config) {
        this(view, config.getName(), view.lazyProvider(config::getFiles), view.lazyProvider(config::getBuildDependencies));
    }

    public DefaultLazyFileCollection(ProjectView view, Configuration config, boolean lenient) {
        this(view, config.getName(), view.lazyProvider(()->{
            if (lenient) {
                return config.getResolvedConfiguration().getLenientConfiguration().getFiles();
            }
            return config.getFiles();
        }), view.lazyProvider(config::getBuildDependencies));
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public Set<File> getFiles() {
        return files.get();
    }

    @Override
    public TaskDependency getBuildDependencies() {
        return taskDependency.get();
    }

}

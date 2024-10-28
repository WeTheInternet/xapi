package net.wti.gradle.internal;

import net.wti.gradle.api.GradleCrossVersionService;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectList;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.DefaultNamedDomainObjectList;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Provider;
import org.gradle.internal.reflect.Instantiator;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Function;

/**
 * GradleCrossVersionServiceDefault:
 * <p>
 * <p>The "for standard gradle 8.X" cross version service.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 27/10/2024 @ 12:33 a.m.
 */
public class GradleCrossVersionServiceModern implements GradleCrossVersionService {
    private TaskDependencyFactory taskDependencyFactory;

    @Override
    public <T> AbstractPublishArtifact publishArtifact(final String name, final T task, final Function<T, File> toFile) {
        return new IntermediateJavaArtifactModern(taskDependencyFactory, name, task) {
            @Override
            public File getFile() {
                return toFile.apply(task);
            }
        };
    }

    @Override
    public <T> AbstractPublishArtifact publishArtifactSources(final String name, final T task, final Function<T, File> toFile) {
        return new IntermediateJavaArtifactModern(taskDependencyFactory, name, task) {
            @Override
            public File getFile() {
                return toFile.apply(task);
            }

            @Override
            public String getExtension() {
                return "jar";
            }

            @Nullable
            @Override
            public String getClassifier() {
                return "sources";
            }
        };
    }

    @Override
    public PublishArtifact lazyArtifact(final Provider<?> task, final String version) {
        return new LazyPublishArtifact(task, version, null, taskDependencyFactory);
    }

    @Override
    public ProjectInternal findProject(final ProjectFinder projectFinder, final String path) {
        return projectFinder.getProject(path);
    }

    @Override
    public <T> NamedDomainObjectList<T> namedDomainList(final Class<T> cls, final Instantiator instantiator) {
        return new DefaultNamedDomainObjectList<>(cls, instantiator, Named.Namer.forType(cls), CollectionCallbackActionDecorator.NOOP);
    }

    @Override
    public void init(final Gradle gradle) {
        taskDependencyFactory = ((GradleInternal)gradle).getServices().get(TaskDependencyFactory.class);
    }
}

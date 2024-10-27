package net.wti.gradle.migrate;

import net.wti.gradle.api.GradleCrossVersionService;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.function.Function;

/**
 * GradleCrossVersionServiceLegacy:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 27/10/2024 @ 1:21 a.m.
 */
public class GradleCrossVersionServiceLegacy implements GradleCrossVersionService {

    @Override
    public <T> AbstractPublishArtifact publishArtifact(final String name, final T task, final Function<T, File> toFile) {
        return new IntermediateJavaArtifactLegacy(name, task) {
            @Override
            public File getFile() {
                return toFile.apply(task);
            }
        };
    }

    @Override
    public <T> AbstractPublishArtifact publishArtifactSources(final String name, final T task, final Function<T, File> toFile) {
        return new IntermediateJavaArtifactLegacy(name, task) {
            @Override
            public File getFile() {
                return toFile.apply(task);
            }

            @Override
            public String getExtension() {
                return "jar";
            }

            @Override
            public String getClassifier() {
                return "sources";
            }
        };
    }

    @Override
    public PublishArtifact lazyArtifact(final Provider<?> task, final String version) {
        return new LazyPublishArtifact(task, version);
    }

    @Override
    public ProjectInternal findProject(final ProjectFinder projectFinder, final String path) {
        return projectFinder.findProject(path);
    }

    @Override
    public void init(final Gradle gradle) {
    }
}

package net.wti.gradle.migrate;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact;
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact;
import org.gradle.api.internal.provider.ProviderInternal;
import org.gradle.api.internal.provider.Providers;
import org.gradle.api.internal.tasks.AbstractTaskDependency;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import java.io.File;
import java.util.Date;

/**
 * LazyPublishArtifact:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 27/10/2024 @ 2:17 a.m.
 */
public class LazyPublishArtifact implements PublishArtifact {
    private final ProviderInternal<?> provider;
    private final String version;
    private PublishArtifact delegate;

    public LazyPublishArtifact(Provider<?> provider) {
        this.provider = Providers.internal(provider);
        this.version = null;
    }

    public LazyPublishArtifact(Provider<?> provider, String version) {
        this.provider = Providers.internal(provider);
        this.version = version;
    }

    public String getName() {
        return this.getDelegate().getName();
    }

    public String getExtension() {
        return this.getDelegate().getExtension();
    }

    public String getType() {
        return this.getDelegate().getType();
    }

    public String getClassifier() {
        return this.getDelegate().getClassifier();
    }

    public File getFile() {
        return this.getDelegate().getFile();
    }

    public Date getDate() {
        return new Date();
    }

    private PublishArtifact getDelegate() {
        if (this.delegate == null) {
            Object value = this.provider.get();
            if (value instanceof FileSystemLocation) {
                FileSystemLocation location = (FileSystemLocation)value;
                this.delegate = this.fromFile(location.getAsFile());
            } else if (value instanceof File) {
                this.delegate = this.fromFile((File)value);
            } else {
                if (!(value instanceof AbstractArchiveTask)) {
                    throw new InvalidUserDataException(String.format("Cannot convert provided value (%s) to a file.", value));
                }

                this.delegate = new ArchivePublishArtifact((AbstractArchiveTask)value);
            }
        }

        return this.delegate;
    }

    private DefaultPublishArtifact fromFile(File file) {
        ArtifactFile artifactFile = new ArtifactFile(file, this.version);
        return new DefaultPublishArtifact(artifactFile.getName(), artifactFile.getExtension(), artifactFile.getExtension(), artifactFile.getClassifier(), (Date)null, file, new Object[0]);
    }

    public TaskDependency getBuildDependencies() {
        return new AbstractTaskDependency() {
            public void visitDependencies(TaskDependencyResolveContext context) {
                context.add(LazyPublishArtifact.this.provider);
            }
        };
    }
}


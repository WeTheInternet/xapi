package net.wti.gradle.internal.impl;

import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact;
import org.gradle.api.plugins.JavaPlugin;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Borrowed from {@link JavaPlugin}; it's a pain it's not visible...
 * hopefully that isn't a warning sign we're ignoring...
 */
public abstract class IntermediateJavaArtifact extends AbstractPublishArtifact {
    private final String type;

    public IntermediateJavaArtifact(String type, Object task) {
        super(task);
        this.type = type;
    }

    @Override
    public String getName() {
        return getFile().getName();
    }

    @Override
    public String getExtension() {
        return "";
    }

    @Override
    public String getType() {
        return type;
    }

    @Nullable
    @Override
    public String getClassifier() {
        return null;
    }

    @Override
    public Date getDate() {
        return null;
    }
}

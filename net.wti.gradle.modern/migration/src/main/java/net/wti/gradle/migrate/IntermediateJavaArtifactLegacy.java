package net.wti.gradle.migrate;

import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact;

import java.util.Date;

/**
 * IntermediateJavaArtifactLegacy:
 * <p>
 * Borrowed from JavaPlugin; it's a pain it's not visible...
 * hopefully that isn't a warning sign we're ignoring...
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 27/10/2024 @ 1:44 a.m.
 */
public abstract class IntermediateJavaArtifactLegacy extends AbstractPublishArtifact {
        private final String type;

        public IntermediateJavaArtifactLegacy(String type, Object task) {
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

        @Override
        public String getClassifier() {
            return null;
        }

        @Override
        public Date getDate() {
            return null;
        }
}

package net.wti.gradle.schema.api;

import net.wti.gradle.internal.api.ProjectView;
import org.gradle.api.Named;
import org.gradle.api.internal.attributes.ImmutableAttributes;

import java.util.Set;

/**
 * A ArchiveConfig describes a single archive-producer within a multi-part project build.
 *
 * For example, an api or spi jar is a build unit.
 * In most cases, the build unit is backed by a sourceset,
 * though this will not be strictly required.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 1:43 PM.
 */
public interface ArchiveConfig extends Named {

    void require(Object ... units);

    Set<String> required();

    boolean isSourceAllowed();

    void setSourceAllowed(boolean allowed);

    ImmutableAttributes getAttributes(ProjectView view);

    PlatformConfig getPlatform();
}

package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.PlatformConfig;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 12:03 AM.
 */
public interface PlatformConfigInternal extends PlatformConfig {

    PlatformConfigInternal getParent();

    @Override
    ArchiveConfigInternal getMainArchive();

    @Override
    default boolean isRoot() {
        return getParent() == null;
    }

    boolean isRequireSource();

    boolean isTest();

    SourceMeta sourceFor(SourceSetContainer srcs, ArchiveConfig archive);

    String configurationName(ArchiveConfig archive);
}

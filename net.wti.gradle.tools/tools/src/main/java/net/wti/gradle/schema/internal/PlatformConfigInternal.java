package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.PlatformConfig;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 12:03 AM.
 */
public interface PlatformConfigInternal extends PlatformConfig {

    @Override
    ArchiveConfigContainerInternal getArchives();

    @Override
    PlatformConfigInternal getParent();

    @Override
    ArchiveConfigInternal getMainArchive();

    @Override
    default boolean isRoot() {
        return getParent() == null;
    }

    @Override
    default ArchiveConfigInternal findArchive(Object named) {
        return (ArchiveConfigInternal)PlatformConfig.super.findArchive(named);
    }

    void baseOn(PlatformConfig rooted);

    void setParent(PlatformConfigInternal myParent);
}

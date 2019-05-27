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
    default PlatformConfigInternal getRoot() {
        // consider overriding this w/ a settable root, and only use this as the default.
        return (PlatformConfigInternal) PlatformConfig.super.getRoot();
    }

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

    @Override
    default ArchiveConfigInternal getArchive(Object named) {
        return (ArchiveConfigInternal) PlatformConfig.super.getArchive(named);
    }

    void baseOn(PlatformConfig rooted);

    void setParent(PlatformConfigInternal myParent);

    boolean isOrReplaces(PlatformConfigInternal argPlat);
}

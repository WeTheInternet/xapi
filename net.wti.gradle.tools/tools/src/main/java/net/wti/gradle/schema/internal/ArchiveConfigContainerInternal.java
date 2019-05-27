package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.ArchiveConfigContainer;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/30/18 @ 12:50 AM.
 */
public interface ArchiveConfigContainerInternal extends ArchiveConfigContainer {
    boolean isWithClassifier();
    boolean isWithCoordinate();
    boolean isWithSourceJar();

    @Override
    ArchiveConfigInternal maybeCreate(String name);

    @Override
    ArchiveConfigInternal getByName(String name);
    // consider doing more *Internal overloads
}

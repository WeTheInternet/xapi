package net.wti.gradle.schema.api;

import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import net.wti.gradle.system.api.RealizableNamedObjectContainer;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 1:46 PM.
 */
public interface ArchiveConfigContainer extends RealizableNamedObjectContainer<ArchiveConfig> {
    void setWithClassifier(boolean classifier);
    void setWithCoordinate(boolean coordinate);
    void setWithSourceJar(boolean sourceJar);

    @Override
    ArchiveConfigInternal maybeCreate(String name);
}

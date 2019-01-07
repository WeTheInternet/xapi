package net.wti.gradle.schema.api;

import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import org.gradle.api.NamedDomainObjectContainer;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 1:46 PM.
 */
public interface ArchiveConfigContainer extends NamedDomainObjectContainer<ArchiveConfig> {
    void setWithClassifier(boolean classifier);
    void setWithCoordinate(boolean coordinate);
    void setWithSourceJar(boolean sourceJar);

    @Override
    ArchiveConfigInternal maybeCreate(String name);
}

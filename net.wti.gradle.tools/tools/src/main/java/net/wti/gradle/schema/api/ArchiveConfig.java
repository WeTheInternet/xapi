package net.wti.gradle.schema.api;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveRequest;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import org.gradle.api.Named;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import xapi.gradle.fu.LazyString;

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

    ArchiveRequest request(ArchiveConfig other, ArchiveRequestType type);

    SetProperty<LazyString> required();

    boolean isSourceAllowed();

    boolean isTest();

    boolean isPublished();

    void setSourceAllowed(Object allowed);

    void setTest(Object allowed);

    void setPublished(Object allowed);

    void setPublishedProvider(Provider<Boolean> allowed);

    ImmutableAttributes getAttributes(ProjectView view);

    PlatformConfig getPlatform();

    default String getPath() {
        return getPlatform().getName() + ":" + getName();
    }
}

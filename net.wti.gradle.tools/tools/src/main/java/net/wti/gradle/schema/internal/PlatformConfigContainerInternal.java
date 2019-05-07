package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.api.PlatformConfigContainer;

import javax.annotation.Nullable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/30/18 @ 4:33 AM.
 */
public interface PlatformConfigContainerInternal extends PlatformConfigContainer {

    @Override
    PlatformConfigInternal maybeCreate(String name);

    ProjectView getView();

    @Nullable
    @Override
    PlatformConfigInternal findByName(String name);
}

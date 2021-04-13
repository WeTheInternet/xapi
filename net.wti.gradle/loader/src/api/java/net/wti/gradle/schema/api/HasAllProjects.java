package net.wti.gradle.schema.api;

import net.wti.gradle.api.MinimalProjectView;
import xapi.fu.data.SetLike;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-06-14 @ 3:54 a.m..
 */
public interface HasAllProjects {

    SchemaCallbacks getCallbacks();

    default void flushWork() {
        getCallbacks().flushCallbacks(this);
    }

    SetLike<SchemaProject> getAllProjects();

    String getGroup();

    String getVersion();

    MinimalProjectView getView();
}

package net.wti.gradle.schema.api;

import xapi.fu.data.SetLike;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-06-14 @ 3:54 a.m..
 */
public interface HasAllProjects {

    SetLike<SchemaProject> getAllProjects();

}

package net.wti.gradle.settings.index;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.tools.HasAllProjects;
import xapi.fu.Out1;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 2:08 a.m..
 */
public interface SchemaIndexer {

    Out1<SchemaIndex> index(MinimalProjectView view, String buildName, HasAllProjects map, final IndexNodePool nodePool);

}


package net.wti.gradle.schema.spi;

import net.wti.gradle.api.MinimalProjectView;
import xapi.fu.Out1;

import java.io.File;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 2:08 a.m..
 */
public interface SchemaIndexer {

    Out1<SchemaIndex> index(MinimalProjectView view, String buildName, File rootDir);

}

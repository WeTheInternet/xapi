package net.wti.gradle.test.api;

import java.io.File;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/15/19 @ 4:12 AM.
 */
public interface HasRootDir {

    File getRootDir();

    default String getDefaultRootProjectName() {
        return getRootDir().getName();
    }
}

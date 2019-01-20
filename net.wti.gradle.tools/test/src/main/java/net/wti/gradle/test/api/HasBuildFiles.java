package net.wti.gradle.test.api;

import java.io.File;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/15/19 @ 4:27 AM.
 */
public interface HasBuildFiles extends HasProjectFiles {

    File getSettingsFile();

}

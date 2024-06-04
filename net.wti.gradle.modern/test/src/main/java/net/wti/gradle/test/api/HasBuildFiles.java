package net.wti.gradle.test.api;

import java.io.File;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/15/19 @ 4:27 AM.
 */
public interface HasBuildFiles extends HasProjectFiles {

    /**
     * 'no.composite' - The system property to set to 'true' to tell tests to skip compositing.
     *
     * The obnoxious name is so you might notice if you accidentally write `-PSKIP_COMPOSITE_SYS_PROP=true`
     *
     * Only useful value to set:
     * `-Dno.composite=true`
     */
    String SKIP_COMPOSITE_SYS_PROP = "no.composite";
    String SKIP_METADATA_SYS_PROP = "no.metadata";
    String DISABLE_COMPOSITE = "-D" + SKIP_COMPOSITE_SYS_PROP + "=true";
    String DISABLE_METADATA = "-D" + SKIP_METADATA_SYS_PROP + "=true";

    File getSettingsFile();

}

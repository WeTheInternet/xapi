package xapi.gradle.task;

import xapi.gradle.api.DefaultArchiveType;
import xapi.gradle.api.PlatformType;

/**
 * Creates a jar tailored for gwt.
 *
 * Published to the `gwt` classifier as part of the build.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/5/18 @ 12:06 AM.
 */
public class GwtJar extends AbstractXapiJar {

    public GwtJar() {
        setArchiveType(PlatformType.GWT);
        setClassifier("gwt");
        addArchiveTypes(DefaultArchiveType.MAIN, DefaultArchiveType.SOURCE, DefaultArchiveType.API);
    }

}

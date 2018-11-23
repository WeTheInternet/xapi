package xapi.gradle.task;

import xapi.gradle.api.DefaultArchiveTypes;

/**
 * Creates a jar tailored for gwt.
 *
 * Published to the `gwt` classifier as part of the build.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/5/18 @ 12:06 AM.
 */
public class GwtJar extends AbstractXapiJar {

    public GwtJar() {
        setArchiveType(DefaultArchiveTypes.GWT);
        setClassifier("gwt");
        addArchiveTypes(DefaultArchiveTypes.MAIN, DefaultArchiveTypes.SOURCE, DefaultArchiveTypes.API);
    }

}

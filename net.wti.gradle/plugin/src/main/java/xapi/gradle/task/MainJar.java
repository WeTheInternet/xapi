package xapi.gradle.task;

import xapi.gradle.api.DefaultArchiveTypes;

/**
 * Creates the "main jar" for a given project.
 *
 * Represents the "published without classifier" artifact of the build,
 * and represents the with-transitive-dependency default artifact (standard "all classes" jar).
 *
 * This should include, if applicable, api, spi or impl types.
 *
 * A "core-only" or shared module should only be exporting api, spi and stub artifacts for its main artifact.
 * Any "impl-only" module should be
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/5/18 @ 12:06 AM.
 */
public class MainJar extends AbstractXapiJar {

    public MainJar() {
        setArchiveType(DefaultArchiveTypes.MAIN);
        setClassifier("gwt");
        addArchiveTypes(DefaultArchiveTypes.API, DefaultArchiveTypes.API);
    }

}

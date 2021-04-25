package xapi.dev.api;

import xapi.dev.impl.ReflectiveMavenLoader;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.mvn.api.MvnDependency;
import xapi.constants.X_Namespace;

import static org.junit.Assert.assertTrue;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/7/17.
 */
public class IsolatedMvnTestThread extends Thread {
    @Override
    public void run() {
        // We are going to force ReflectiveMavenLoader to find a jar...
        // (If your local maven repo is clean, we will download from central)
        ReflectiveMavenLoader loader = new ReflectiveMavenLoader();
        System.setProperty(X_Namespace.PROPERTY_MAVEN_UNRESOLVABLE, ".*dev-javac.*");
        final MvnDependency dep = loader.getDependency("xapi-dev-javac");
        final SizedIterable<String> result = MappedIterable.mapped(loader.downloadDependency(dep).out1())
            .counted();
        assertTrue(result.anyMatch(String::contains, "xapi-dev-javac"));
    }
}

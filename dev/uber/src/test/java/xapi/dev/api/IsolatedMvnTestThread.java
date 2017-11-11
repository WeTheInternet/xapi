package xapi.dev.api;

import xapi.dev.impl.ReflectiveMavenLoader;
import xapi.fu.MappedIterable;
import xapi.fu.iterate.SizedIterable;
import xapi.mvn.api.MvnDependency;

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
        final MvnDependency dep = loader.getDependency("xapi-dev-javac");
        final SizedIterable<String> result = MappedIterable.mapped(loader.downloadDependency(dep).out1())
            .counted();
        assertTrue(result.anyMatch(String::contains, "xapi-dev-javac"));
    }
}

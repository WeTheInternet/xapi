package xapi.dev.api;

import org.junit.Assert;
import org.junit.Test;
import xapi.fu.MappedIterable;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.iterate.SizedIterable;
import xapi.mvn.api.MvnDependency;
import xapi.reflect.X_Reflect;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static xapi.mvn.X_Maven.downloadDependencies;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/26/17.
 */
public class MavenLoaderTest {

    @Test
    public void testSimpleLoad() {
        // Since we are doing our testing in the uber module,
        // X_Maven will be on the classpath, so MavenLoader will have a much easier time.
        MavenLoader loader = new MavenLoader();
        final MvnDependency dep = loader.getDependency("xapi-fu");
        final Out1<Iterable<String>> result = loader.downloadDependency(dep);
        final Iterable<String> items = result.out1();
        final SizedIterable<String> counted = MappedIterable.mapped(items).counted();
        assertEquals("Expected only 2 result", 2, counted.size());
        assertTrue("Expected xapi-fu in result",
            counted.hasMatch2(String::contains, "xapi-fu")
        );

        assertTrue("Expected validation api in result",
            counted.hasMatch2(String::contains, "validation")
        );
    }

    @Test
    public void testIsolatedLoad() throws Throwable {
        // Lets make it harder for MavenLoader...  take away the uber module classpath.
        MavenLoader loader = new MavenLoader();
        Mutable<Throwable> failure = new Mutable<>(null);
        final HashSet<String> dedup = new HashSet<>();
        final URL[] classpath =
            downloadDependencies(loader.getDependency("xapi-dev-api"))
            .out1()
            .append(downloadDependencies(loader.getDependency("xapi-jre-model")).out1())
            .appendItems(X_Reflect.getFileLoc(IsolatedThread.class), X_Reflect.getFileLoc(Assert.class))
            .map(s->"file:" + s)
            .filterOnce(dedup::add)
            .mapUnsafe(URL::new)
            .toArray(URL[]::new);
        final URLClassLoader isolated = new URLClassLoader(classpath, null);
        Thread thread = (Thread) X_Reflect.construct(isolated.loadClass(IsolatedThread.class.getName()), new Class[]{});
        thread.setContextClassLoader(isolated);
        thread.setUncaughtExceptionHandler((t,e)-> failure.in(e) );
        thread.start();
        thread.join();
        if (failure.isNonNull()) {
            throw failure.out1();
        }
    }
}
class IsolatedThread extends Thread {
    @Override
    public void run() {
        // We are going to force MavenLoader to find a jar...
        // (If your local maven repo is clean, we will download from central)
        MavenLoader loader = new MavenLoader();
        final MvnDependency dep = loader.getDependency("xapi-dev-javac");
        final SizedIterable<String> result = MappedIterable.mapped(loader.downloadDependency(dep).out1())
            .counted();
        assertTrue(result.anyMatch(String::contains, "xapi-dev-javac"));
    }
}

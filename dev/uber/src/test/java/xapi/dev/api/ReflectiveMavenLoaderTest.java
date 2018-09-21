package xapi.dev.api;

import org.junit.Assert;
import org.junit.Test;
import xapi.dev.impl.ReflectiveMavenLoader;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.itr.SizedIterable;
import xapi.mvn.api.MvnDependency;
import xapi.reflect.X_Reflect;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static xapi.mvn.X_Maven.downloadDependencies;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/26/17.
 */
public class ReflectiveMavenLoaderTest {

    @Test
    public void testSimpleLoad() {
        // Since we are doing our testing in the uber module,
        // X_Maven will be on the classpath, so ReflectiveMavenLoader will have a much easier time.
        ReflectiveMavenLoader loader = new ReflectiveMavenLoader();
        final MvnDependency dep = loader.getDependency("xapi-fu");
        final Out1<Iterable<String>> result = loader.downloadDependency(dep);
        final Iterable<String> items = result.out1();
        final SizedIterable<String> counted = MappedIterable.mapped(items).counted();
        if (counted.size() == 2) {
            // we loaded jars
            assertTrue("Expected xapi-fu in result",
                counted.hasMatch2(String::contains, "xapi-fu")
            );
        } else if (counted.size() == 4) {
            // we loaded source folders
            assertTrue("Expected xapi-fu in result",
                counted
                    .map(s->s.replace('\\', '/'))
                    .hasMatch2(String::contains, "core/fu/src/main/java")
            );
        } else {
            fail("Unexpected number of results for xapi-fu:\n" + counted.join("\n"));
        }

        assertTrue("Expected validation api in result",
            counted.hasMatch2(String::contains, "validation")
        );
    }

    @Test
    public void testIsolatedLoad() throws Throwable {
        // Lets make it harder for ReflectiveMavenLoader...  take away the uber module classpath.
        ReflectiveMavenLoader loader = new ReflectiveMavenLoader();
        Mutable<Throwable> failure = new Mutable<>(null);
        final HashSet<String> dedup = new HashSet<>();
        final URL[] classpath =
            downloadDependencies(loader.getDependency("xapi-dev-api"))
            .out1()
            .append(downloadDependencies(loader.getDependency("xapi-jre-model")).out1())
            .appendItems(X_Reflect.getFileLoc(IsolatedMvnTestThread.class), X_Reflect.getFileLoc(Assert.class))
            .map(s->"file://" + s)
            .filterOnce(dedup::add)
            .mapUnsafe(URL::new)
            .toArray(URL[]::new);
        final URLClassLoader isolated = new URLClassLoader(classpath, null);
        final String name = IsolatedMvnTestThread.class.getName();
        final Class<?> threadType = isolated.loadClass(name);
        Thread thread = (Thread) X_Reflect.construct(threadType, new Class[]{});
        thread.setContextClassLoader(isolated);
        thread.setUncaughtExceptionHandler((t,e)-> failure.in(e) );
        thread.start();
        thread.join();
        if (failure.isNonNull()) {
            throw failure.out1();
        }
    }
}

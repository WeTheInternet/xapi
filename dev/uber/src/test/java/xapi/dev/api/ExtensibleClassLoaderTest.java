package xapi.dev.api;

import org.junit.Test;
import xapi.fu.Do;
import xapi.reflect.X_Reflect;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/7/17.
 */
public class ExtensibleClassLoaderTest {

    public static class TheClass {
        public static final int num = System.identityHashCode(ExtensibleClassLoaderTest.class);
    }

    @Test
    public void testStaticIsolation() throws MalformedURLException, InterruptedException {
        int myNum = TheClass.num;
        String clName = TheClass.class.getName();

        final URL[] urls = new URL[]{new URL("file:" + X_Reflect.getFileLoc(TheClass.class))};
        ExtensibleClassLoader isolated = new ExtensibleClassLoader(urls, null)
            .checkMeFirst();
        ExtensibleClassLoader sharedCheckFirst = new ExtensibleClassLoader(urls, Thread.currentThread().getContextClassLoader())
            .checkMeFirst();
        ExtensibleClassLoader sharedCheckLast = new ExtensibleClassLoader(urls, Thread.currentThread().getContextClassLoader());
        int[] results = new int[3];
        Thread[] threads = {
            new Thread(Do.unsafe(()->
                results[0] = Thread.currentThread().getContextClassLoader().loadClass(clName).getField("num").getInt(null)
            ).toRunnable()),
            new Thread(Do.unsafe(()->
                results[1] = Thread.currentThread().getContextClassLoader().loadClass(clName).getField("num").getInt(null)
            ).toRunnable()),
            new Thread(Do.unsafe(()->
                results[2] = Thread.currentThread().getContextClassLoader().loadClass(clName).getField("num").getInt(null)
            ).toRunnable()),
        };
        threads[0].setContextClassLoader(isolated);
        threads[1].setContextClassLoader(sharedCheckFirst);
        threads[2].setContextClassLoader(sharedCheckLast);

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertNotEquals("Isolated loader saw our value", myNum, results[0]);
        assertNotEquals("Shared w/ check-first did not create a new class", myNum, results[1]);
        assertEquals("Shared w/ check-last did not reuse current value", myNum, results[2]);

    }

}

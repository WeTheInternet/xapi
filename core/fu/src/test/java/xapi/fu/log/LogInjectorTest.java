package xapi.fu.log;

import org.junit.Assert;
import org.junit.Test;
import xapi.fu.FuTest;
import xapi.fu.log.Log.LogLevel;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/20/18 @ 11:26 PM.
 */
public class LogInjectorTest extends FuTest {

    @Test
    public void testDefaultLogging() throws Throwable {
        // Make sure basic logging works OOTB
        final String msg = "Success! " + hashCode();
        borrowSout(
            ()-> LogInjector.DEFAULT.log(LogLevel.INFO, msg)
            , lines->
                Assert.assertEquals("[INFO] " + msg, lines.first())
        );
        assertTrue("Failed", !failed);
        // just for good measure, actually print another line to the real stdOut
        Log.defaultLogger().log(LogInjectorTest.class, "Default Logging Works!");
    }

    @Test
    public void testInjectedLogging() throws Throwable {
        Path dir = Files.createTempDirectory("injectedLogging");
        final Path singletons = dir.resolve("META-INF/singletons");
        Files.createDirectories(singletons);
        final Path manifest = singletons.resolve(LogInjector.class.getName());
        Files.createFile(manifest);
        Files.write(manifest, "xapi.fu.log.TestInjector".getBytes());

        final ClassLoader cl = new URLClassLoader(new URL[]{
            dir.toUri().toURL(),
            LogInjector.class.getProtectionDomain().getCodeSource().getLocation(),
            LogInjectorTest.class.getProtectionDomain().getCodeSource().getLocation(),
        }, null);

        final Class<?> clazz = cl.loadClass(TestInjectorThread.class.getName());
        final Object inst = clazz.newInstance();
        Thread t = (Thread) inst;

        t.setContextClassLoader(cl);
        t.start();
        t.join();

        final Object result = t.getClass().getMethod("isSuccess").invoke(t);
        assertTrue("Failed in thread", Boolean.TRUE.equals(result));
        assertTrue("Failed", !failed);
    }

}

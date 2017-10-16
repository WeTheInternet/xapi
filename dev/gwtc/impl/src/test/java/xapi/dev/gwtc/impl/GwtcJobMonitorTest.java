package xapi.dev.gwtc.impl;

import org.junit.Test;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.fu.Out1;
import xapi.reflect.X_Reflect;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.Assert.assertEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/13/17.
 */
public class GwtcJobMonitorTest {

    @Test
    public void testIsolatedClassLoader() throws
                                          Throwable {

        final LinkedBlockingDeque<String> caller = new LinkedBlockingDeque<>();
        final LinkedBlockingDeque<String> compiler = new LinkedBlockingDeque<>();

        final URL url1 = new URL("file:" + X_Reflect.getFileLoc(GwtcJobMonitorImpl.class));
        final URL url2 = new URL("file:" + X_Reflect.getFileLoc(GwtcJobMonitor.class));
        final URL url3 = new URL("file:" + X_Reflect.getFileLoc(Out1.class));
        final ClassLoader cl = new URLClassLoader(new URL[]{url1, url2, url3}, null);
        final GwtcJobMonitor monitor = GwtcJobMonitorImpl.newMonitor(cl, caller, compiler);

        // First, lets ensure local classloader is wired up sanely
        caller.put("start");
        assertEquals("", "start", monitor.readAsCompiler());
        compiler.put("finish");
        assertEquals("", "finish", monitor.readAsCaller());
        monitor.writeAsCaller("begin");
        assertEquals("", "begin", caller.take());
        monitor.writeAsCompiler("end");
        assertEquals("", "end", compiler.take());

        // Now, lets try using the monitor in a thread with an isolated classloader
        caller.put("hello");
        Throwable[] error = {null};
        String[] received = {null, null, null};
        final UncaughtExceptionHandler eh = (t, e) -> {
            error[0] = e;
        };
        CountDownLatch firstCall = new CountDownLatch(1), secondCall = new CountDownLatch(1);
        Thread t = new Thread() {
            @Override
            public void run() {
                String got = monitor.readAsCompiler();
                received[0] = got;
                firstCall.countDown();
                monitor.writeAsCompiler("world");
                monitor.writeAsCompiler("again");
                try {
                    caller.put("finished");
                    secondCall.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                received[0] = monitor.readAsCompiler();
            }
            {
                setName("Foreign Thread");
                setDaemon(false);
                setUncaughtExceptionHandler(eh);
                setContextClassLoader(cl);
                start();
            }
        };
        firstCall.await();
        assertEquals("", "hello", received[0]);
        String reply = monitor.readAsCaller();
        assertEquals("", "world", reply);
        String fromDeque = compiler.take();
        assertEquals("", "again", fromDeque);
        secondCall.await();
        assertEquals("", "finished", received[0]);
        caller.put("done");
        t.join();
        assertEquals("", "done", monitor.readAsCompiler());
        if (error[0] != null) {
            throw error[0];
        }
    }
}

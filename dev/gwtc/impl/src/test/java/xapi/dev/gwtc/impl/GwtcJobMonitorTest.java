package xapi.dev.gwtc.impl;

import org.junit.Assert;
import org.junit.Test;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.fu.Out1;
import xapi.reflect.X_Reflect;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.Assert.assertEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/13/17.
 */
public class GwtcJobMonitorTest {

    public static class IsolatedThread extends Thread {

        private final GwtcJobMonitor remote;
        private final String[] received;
        private final LinkedBlockingDeque<String> caller;
        private final LinkedBlockingDeque<String> compiler;

        public IsolatedThread(
            Object remoteMonitor,
            ClassLoader cl,
            UncaughtExceptionHandler eh,
            String[] received,
            LinkedBlockingDeque<String> caller,
            LinkedBlockingDeque<String> compiler
        ) {
            remote = (GwtcJobMonitor) remoteMonitor;
            this.received = received;
            this.caller = caller;
            this.compiler = compiler;
            setName("Foreign Thread");
            setDaemon(false);
            setUncaughtExceptionHandler(eh);
            setContextClassLoader(cl);
            start();
        }

        @Override
        public void run() {
            System.setProperty("x", "z");
            received[0] = remote.readAsCompiler();
            assertEquals("hello", received[0]);
            remote.writeAsCompiler("world");
            try {
                final String fromCaller = caller.take();
                received[1] = fromCaller;
                assertEquals("I said hello!", received[1]);
                compiler.put("hello to you too");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            received[2] = remote.readAsCompiler();
            assertEquals("I'm finished", received[2]);
            remote.writeAsCompiler("goodbye");
        }
    }

    @Test
    public void testIsolatedClassLoader() throws
                                          Throwable {

        final LinkedBlockingDeque<String> caller = new LinkedBlockingDeque<>();
        final LinkedBlockingDeque<String> compiler = new LinkedBlockingDeque<>();

        // We will use the monitor in a thread with an isolated classloader:
        final URL url1 = new URL("file:" + X_Reflect.getFileLoc(GwtcJobMonitorImpl.class));
        final URL url2 = new URL("file:" + X_Reflect.getFileLoc(GwtcJobMonitor.class));
        final URL url3 = new URL("file:" + X_Reflect.getFileLoc(Out1.class));
        final URL url4 = new URL("file:" + X_Reflect.getFileLoc(IsolatedThread.class));
        final URL url5 = new URL("file:" + X_Reflect.getFileLoc(Assert.class));
        final ClassLoader cl = new URLClassLoader(new URL[]{url1, url2, url3, url4, url5}, null);
        final GwtcJobMonitorImpl local = new GwtcJobMonitorImpl(caller, compiler);
        System.setProperty("x", "y");
        // First, lets ensure local classloader is wired up sanely
        caller.put("start");
        assertEquals("", "start", local.readAsCompiler());
        compiler.put("finish");
        assertEquals("", "finish", local.readAsCaller());
        local.writeAsCaller("begin");
        assertEquals("", "begin", caller.take());
        local.writeAsCompiler("end");
        assertEquals("", "end", compiler.take());

        Throwable[] error = {null};
        String[] received = {null, null, null};
        final UncaughtExceptionHandler eh = (t, e) -> {
            e.printStackTrace();
            error[0] = e;
        };
        Object remoteMonitor = local.forClassloader(cl, caller, compiler);
        Thread t = (Thread) cl.loadClass(IsolatedThread.class.getName())
            .getConstructor(Object.class, ClassLoader.class, UncaughtExceptionHandler.class,
                String[].class, LinkedBlockingDeque.class, LinkedBlockingDeque.class)
            .newInstance(remoteMonitor, cl, eh, received, caller, compiler);

        local.writeAsCaller("hello");
        String forCaller = local.readAsCaller(); // world

        assertEquals("", "hello", received[0]);
        assertEquals("", "world", forCaller);

        caller.put("I said hello!");
        String fromDeque = compiler.take();
        assertEquals("", "hello to you too", fromDeque);
        assertEquals("", "I said hello!", received[1]);

        local.writeAsCaller("I'm finished");
        String goodbye = local.readAsCaller();

        // In order to check what the other thread wrote, we write to an array of results;
        // for blocking, we rely on a simple latch
        assertEquals("", "goodbye", goodbye);

        t.join(5000);
        System.out.println(System.getProperty("x"));
        if (error[0] != null) {
            throw error[0];
        }
    }
}

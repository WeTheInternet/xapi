package xapi.dev.gwtc.impl;

import org.junit.Assert;
import org.junit.Test;
import xapi.bytecode.MemberInfo;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.resource.api.ClasspathResource;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.jre.inject.RuntimeInjector;
import xapi.log.api.LogService;
import xapi.process.X_Process;
import xapi.reflect.X_Reflect;
import xapi.reflect.service.ReflectionService;
import xapi.time.X_Time;

import javax.inject.Provider;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

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

    @Test(timeout = 200_000)
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
        final URL url6 = new URL("file:" + X_Reflect.getFileLoc(X_Time.class));
        final URL url7 = new URL("file:" + X_Reflect.getFileLoc(X_Inject.class));
        final URL url8 = new URL("file:" + X_Reflect.getFileLoc(NotConfiguredCorrectly.class));
        final URL url9 = new URL("file:" + X_Reflect.getFileLoc(LogService.class));
        final URL url10 = new URL("file:" + X_Reflect.getFileLoc(Provider.class));
        final URL url11 = new URL("file:" + X_Reflect.getFileLoc(RuntimeInjector.class));
        final URL url12 = new URL("file:" + X_Reflect.getFileLoc(ClasspathScanner.class));
        final URL url13 = new URL("file:" + X_Reflect.getFileLoc(ReflectionService.class));
        final URL url14 = new URL("file:" + X_Reflect.getFileLoc(MemberInfo.class));
        final URL url15 = new URL("file:" + X_Reflect.getFileLoc(ClasspathResource.class));
        final ClassLoader cl = new URLClassLoader(new URL[]{
            url1, url2, url3, url4, url5, url6, url7, url8, url9, url10, url11, url12, url13, url14, url15
        }, null);
        X_Process.scheduleInterruption(20, TimeUnit.SECONDS);
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

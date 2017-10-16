package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.Out1;
import xapi.fu.Out1.Out1Unsafe;
import xapi.fu.X_Fu;
import xapi.gwtc.api.CompiledDirectory;
import xapi.log.X_Log;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.PrimitiveSerializerDefault;
import xapi.source.api.CharIterator;
import xapi.util.X_Debug;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.SortedSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.google.gwt.dev.CompileTaskRunner;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.Compiler.ArgProcessor;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.CompilerOptionsImpl;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.codeserver.CompileStrategy;

/**
 * This runs in the same classpath as the running Gwt compile, and is responsible
 * for monitoring the state of the compile, pushing results back to a {@link GwtcJobController}.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/12/17.
 */
public class GwtcJobMonitorImpl implements GwtcJobMonitor {

    private In1Unsafe<String> writeAsCaller;
    private Out1Unsafe<String> readAsCaller;
    private In1Unsafe<String> writeAsCompiler;
    private Out1Unsafe<String> readAsCompiler;
    private Out1<Boolean> hasCallerOutput;
    private Out1<Boolean> hasCompilerOutput;

    @SuppressWarnings("unused") // called reflectively
    public GwtcJobMonitorImpl(Object callerDeque, Object compilerDeque)
    throws InterruptedException, NoSuchMethodException {
        // We were instantiated directly, so assume we are running in a foreign classloader,
        // and that our parameters are a pair of LinkedBlockingDeque's that we can use to communicate with.
        BlockingDeque<String> caller, compiler;
        caller = proxy(callerDeque);
        compiler = proxy(compilerDeque);
        readAsCaller = compiler::take;
        readAsCompiler = caller::take;
        writeAsCaller = caller::put;
        writeAsCompiler = compiler::put;
        hasCallerOutput = ()->!caller.isEmpty();
        hasCompilerOutput = ()->!compiler.isEmpty();
    }

    public GwtcJobMonitorImpl(InputStream in, PrintStream out) {
        // Created from a java main; we will only handle *AsCompiler
    }

    public GwtcJobMonitorImpl(Out1Unsafe<String> in, In1Unsafe<String> out) {
        // Created to call into a java main; we will only handle *AsCaller
    }

    public static GwtcJobMonitor newMonitor(ClassLoader loader) {
        return newMonitor(loader, new LinkedBlockingDeque<>(), new LinkedBlockingDeque<>());
    }
    public static GwtcJobMonitor newMonitor(ClassLoader loader, LinkedBlockingDeque<String> input, LinkedBlockingDeque<String> output) {
        try {

            final Class<?> foreignClass = loader.loadClass(GwtcJobMonitorImpl.class.getName());
            final Constructor<?> ctor = foreignClass.getConstructor(Object.class, Object.class);
            final Object created = ctor.newInstance(input, output);
            final Method writeToCaller = foreignClass.getMethod("writeAsCaller", String.class);
            final Method readFromCaller = foreignClass.getMethod("readAsCaller");
            final Method writeToCompiler = foreignClass.getMethod("writeAsCompiler", String.class);
            final Method readFromCompiler = foreignClass.getMethod("readAsCompiler");
            final ClassLoader callingLoader = Thread.currentThread().getContextClassLoader();
        return (GwtcJobMonitor) Proxy.newProxyInstance(callingLoader, new Class[]{GwtcJobMonitor.class}, (proxy, method, args)->{
            // Here is where we handle calling into the monitor instance from calling classloader.
            switch (method.getName()) {
                case "writeAsCaller":
                    return writeToCaller.invoke(created, args);
                case "readAsCaller":
                    return readFromCaller.invoke(created, args);
                case "writeAsCompiler":
                    return writeToCompiler.invoke(created, args);
                case "readAsCompiler":
                    return readFromCompiler.invoke(created, args);

            }
            throw new IllegalArgumentException("Unhandled method invocation " + method.toGenericString() + " for GwtcJobMonitor proxy");
        });
        } catch (Exception e) {
            throw X_Debug.rethrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private BlockingDeque<String> proxy(Object inputDeque) throws NoSuchMethodException {
        if (inputDeque instanceof BlockingDeque) {
            return (BlockingDeque<String>) inputDeque;
        }
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();

        final Method put = inputDeque.getClass().getMethod("put", Object.class);
        final Method take = inputDeque.getClass().getMethod("take");
        final Method isEmpty = inputDeque.getClass().getMethod("isEmpty");

        return (BlockingDeque<String>)Proxy.newProxyInstance(cl, new Class[]{BlockingDeque.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "put":
                    return put.invoke(inputDeque, args);
                case "take":
                    return take.invoke(inputDeque, args);
                case "isEmpty":
                    return isEmpty.invoke(inputDeque, args);
            }
            throw new UnsupportedOperationException(getClass() + " does not support " + method.toGenericString());
        });
    }

    @Override
    public String readAsCaller() {
        return readAsCaller.out1();
    }

    @Override
    public void writeAsCaller(String toCaller) {
        writeAsCaller.in(toCaller);
    }

    @Override
    public String readAsCompiler() {
        return readAsCompiler.out1();
    }

    @Override
    public void writeAsCompiler(String toCompiler) {
        writeAsCompiler.in(toCompiler);
    }
}

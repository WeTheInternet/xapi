package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.Out1;
import xapi.fu.Out1.Out1Unsafe;
import xapi.fu.X_Fu;
import xapi.fu.iterate.ArrayIterable;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.PrimitiveSerializerDefault;
import xapi.reflect.X_Reflect;
import xapi.source.X_Modifier;
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
import java.net.URLClassLoader;
import java.util.SortedSet;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import static xapi.fu.iterate.ArrayIterable.iterate;

import com.google.gwt.dev.CompileTaskRunner;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.Compiler.ArgProcessor;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.CompilerOptionsImpl;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.codeserver.CompileStrategy;
import com.google.gwt.reflect.shared.GwtReflect;

/**
 * This runs in the same classpath as the running Gwt compile, and is responsible
 * for monitoring the state of the compile, pushing results back to a {@link GwtcJobController}.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/12/17.
 */
public class GwtcJobMonitorImpl implements GwtcJobMonitor {

    private final In1Unsafe<String> writeAsCaller;
    private final Out1Unsafe<String> readAsCaller;
    private final In1Unsafe<String> writeAsCompiler;
    private final Out1Unsafe<String> readAsCompiler;
    private final Out1Unsafe<Boolean> hasCallerOutput;
    private final Out1Unsafe<Boolean> hasCompilerOutput;

    @SuppressWarnings("unused") // called reflectively
    public GwtcJobMonitorImpl(Object callerDeque, Object compilerDeque)
    throws InterruptedException, NoSuchMethodException {
        this(proxy(callerDeque), proxy(compilerDeque));
    }

    public GwtcJobMonitorImpl(BlockingDeque<String> caller, BlockingDeque<String> compiler) {
        // We were instantiated directly, so assume we are running in a foreign classloader,
        // and that our parameters are a pair of LinkedBlockingDeque's that we can use to communicate with.
        readAsCaller = compiler::take;
        readAsCompiler = caller::take;
        writeAsCaller = caller::put;
        writeAsCompiler = compiler::put;
        hasCallerOutput = ()->!caller.isEmpty();
        hasCompilerOutput = ()->!compiler.isEmpty();
    }

    public GwtcJobMonitorImpl(InputStream in, PrintStream out) {
        // Created from a java main; we will only handle *AsCompiler
        readAsCompiler = ()->X_IO.readLine(in);
        writeAsCompiler = line->{
            if (line.endsWith("\n")) {
                out.print(line);
            } else {
                out.println(line);
            }
        };
        hasCallerOutput = ()->in.available() > 0;
        readAsCaller = ()->{throw notSupported("caller");};
        writeAsCaller = ignored->{throw notSupported("caller");};
        hasCompilerOutput = ()->{throw notSupported("caller");};
    }

    public GwtcJobMonitorImpl(Out1Unsafe<String> in, In1Unsafe<String> out, Out1Unsafe<Boolean> hasMessage) {
        // Created to call into a java main; we will only handle *AsCaller
        readAsCaller = in;
        writeAsCaller = out;
        hasCompilerOutput = hasMessage;
        readAsCompiler = ()->{throw notSupported("compiler");};
        writeAsCompiler = ignored->{throw notSupported("compiler");};
        hasCallerOutput = ()->{throw notSupported("compiler");};
    }

    private UnsupportedOperationException notSupported(String compiler) {
        return new UnsupportedOperationException(getClass().getName() + " does not support use of the " + compiler + " channel");
    }

    @SuppressWarnings("unchecked")
    private static BlockingDeque<String> proxy(Object inputDeque) throws NoSuchMethodException {
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
            throw new UnsupportedOperationException(method.toGenericString() + " is not supported");
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

    @Override
    public boolean hasMessageForCaller() {
        return Boolean.TRUE.equals(hasCompilerOutput.out1());
    }

    @Override
    public boolean hasMessageForCompiler() {
        return Boolean.TRUE.equals(hasCallerOutput.out1());
    }

    public Object forClassloader(ClassLoader loader, Object input, Object output) {
        if (loader == getClass().getClassLoader()) {
            return this;
        }
        try {
            final Class<?> foreignInterface = loader.loadClass(GwtcJobMonitor.class.getName());
            final Class<?> foreignClass = loader.loadClass(GwtcJobMonitorImpl.class.getName());
            final Constructor<?> ctor = foreignClass.getConstructor(Object.class, Object.class);
            final Object created = ctor.newInstance(input, output);
            final Method hasMessageForCaller = foreignClass.getMethod("hasMessageForCaller");
            final Method writeToCaller = foreignClass.getMethod("writeAsCaller", String.class);
            final Method readFromCaller = foreignClass.getMethod("readAsCaller");
            final Method writeToCompiler = foreignClass.getMethod("writeAsCompiler", String.class);
            final Method readFromCompiler = foreignClass.getMethod("readAsCompiler");
            final Method hasMessageForCompiler = foreignClass.getMethod("hasMessageForCompiler");

            final Class<?> statusClass = loader.loadClass(CompileStatus.class.getName());
            final ClassLoader callingLoader = Thread.currentThread().getContextClassLoader();
            final Class<?> monitorClass = loader.loadClass(GwtcJobMonitor.class.getName());
            assert loader.loadClass("java.lang.Enum") == Enum.class;
            final Method getEnumName = Enum.class.getMethod("name");
            final Method updateCompileStatus = monitorClass.getMethod("updateCompileStatus",
                statusClass, String[].class);
            final Method getCompileStatus = statusClass.getMethod("valueOf", String.class);

            return Proxy.newProxyInstance(loader, new Class[]{
                foreignInterface
            }, (proxy, method, args)->{
                // Here is where we handle calling into the monitor instance from compiler classloader.
                switch (method.getName()) {
                    case "writeAsCaller":
                        return writeToCaller.invoke(created, args);
                    case "writeAsCompiler":
                        return writeToCompiler.invoke(created, args);
                    case "readAsCaller":
                        return readFromCaller.invoke(created, args);
                    case "readAsCompiler":
                        return readFromCompiler.invoke(created, args);
                    case "updateCompileStatus":
                        final Object enumType;
                        Object[] newArgs = new Object[]{args[0], args[1]};

                        if (Thread.currentThread().getContextClassLoader() == loader) {
                            final Object name = getEnumName.invoke(args[0]);
                            args[0] = getCompileStatus.invoke(null, name);
                        }
                        return updateCompileStatus.invoke(created, newArgs);
                    default:
                        final Class[] proxiedArgTypes =
                            iterate(method.getParameterTypes())
                                .map2(X_Reflect::rebase, callingLoader)
                                .toArray(Class[]::new);
                        if (method.isDefault()) {
                            return X_Reflect.invokeDefaultMethod(foreignClass, method.getName(), proxiedArgTypes, proxy, args);
                        }
                        return X_Reflect.invoke(foreignClass, method.getName(), proxiedArgTypes, created, args == null ? X_Fu.emptyArray() : args);
                }
            });
        } catch (Exception e) {
            throw X_Debug.rethrow(e);
        }
    }
}

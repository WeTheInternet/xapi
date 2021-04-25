package xapi.javac.dev.api;

import xapi.fu.In1;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.Out2;
import xapi.javac.dev.model.CompilerSettings;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.source.X_Source;
import xapi.util.X_Debug;

import javax.annotation.processing.Processor;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static xapi.string.X_String.classToSourceFiles;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/30/16.
 */
public class PendingCompile {

    private final CompilerSettings settings;
    private final CompilerService compiler;
    private final Map<String, Class<?>> javaFiles = new LinkedHashMap<>();
    private final boolean test;

    public PendingCompile(Class<?> cls, CompilerService compiler) {
        this.settings = compiler.settingsForClass(cls);
        this.compiler = compiler;

        String loc = X_Reflect.getFileLoc(cls);
        test = X_Source.hasTestPath(loc);
        final String canonicalFile = normalizeLocation(loc, test) + classToSourceFiles(cls);
        javaFiles.put(canonicalFile, cls);
    }

    protected String normalizeLocation(String loc, boolean test) {
        if (test) {
            loc = X_Source.rebaseTest(loc, "src/test/java");
        } else {
            loc = X_Source.rebaseMain(loc, "src/main/java");
        }
        return loc;
    }

    public PendingCompile addProcessor(Class<? extends Processor> processor) {
        // TODO consider using the manual -processor ClassName override instead of this...
        String existing = settings.getProcessorPath();
        final String loc = X_Reflect.getFileLoc(processor);
        if (existing == null) {
            settings.setProcessorPath(loc);
        } else if (!existing.contains(loc)) {
            existing = existing + File.pathSeparator + loc;
            settings.setProcessorPath(existing);
        }
        return this;
    }

    public Out2<Integer,URL> compile(){
        return compiler.compileFiles(settings, javaFiles.keySet().toArray(new String[javaFiles.size()]));
    }

    public Out2<Integer, URL> compileAndRun(In2Unsafe<ClassLoader, List<Class<?>>> callback) {
        return compileAndRun(callback, true);
    }

    @SuppressWarnings("varargs")
    public Out2<Integer, URL> compileAndRun(In2Unsafe<ClassLoader, List<Class<?>>> callback, boolean synchronous) {
        final Out2<Integer, URL> result = compile();

        assert result.out1() == 0 : "Annotation processing failed";

        final URLClassLoader cl = new URLClassLoader(new URL[]{result.out2()}, Thread.currentThread().getContextClassLoader());
        Thread runIn = new Thread(()->{
            try {
                List<Class<?>> loaded = new ArrayList<>();
                for (Class<?> cls : javaFiles.values()) {
                    loaded.add(cl.loadClass(cls.getCanonicalName()));
                }
                callback.in(cl, loaded);
            } catch (ClassNotFoundException e) {
                X_Log.error(getClass(), "Compilation did not succeed for classes", javaFiles, e );
                throw X_Debug.rethrow(e);
            }
        });
        runIn.setContextClassLoader(cl);
        runIn.start();
        if (synchronous) {
            try {
                runIn.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw X_Debug.rethrow(e);
            }
        }

        return result;
    }

    public CompilerSettings getSettings() {
        return settings;
    }

    public PendingCompile withSettings(In1<CompilerSettings> callback) {
        callback.in(getSettings());
        return this;
    }

    public CompilerService getCompiler() {
        return compiler;
    }

    public PendingCompile withCompiler(In1<CompilerService> callback) {
        callback.in(getCompiler());
        return this;
    }
}

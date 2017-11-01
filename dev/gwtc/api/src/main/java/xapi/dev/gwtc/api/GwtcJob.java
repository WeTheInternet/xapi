package xapi.dev.gwtc.api;

import xapi.dev.api.ExtensibleClassLoader;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileStatus;
import xapi.except.MultiException;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.Do;
import xapi.fu.In2;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.model.api.PrimitiveSerializer;
import xapi.process.X_Process;
import xapi.reflect.X_Reflect;
import xapi.source.api.CharIterator;
import xapi.source.api.Chars;
import xapi.util.X_Debug;
import xapi.util.api.Destroyable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static xapi.dev.gwtc.api.GwtcJobMonitor.CompileStatus.*;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.codeserver.CompileStrategy;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public abstract class GwtcJob implements Destroyable {

    private final String moduleName;
    private final String moduleShortName;
    private CompileStatus state;
    private final boolean recompiler;
    private final boolean j2cl;
    private CompiledDirectory directory;
    private final ChainBuilder<In2<CompiledDirectory, Throwable>> callbacks;
    private Throwable error;
    private Throwable callbackError;

    public GwtcJob(GwtManifest manifest) {
        this(manifest.getModuleName(), manifest.getModuleShortName(), manifest.isRecompile(), manifest.isJ2cl());
    }

    public GwtcJob(String moduleName, String moduleShortName, boolean recompiler, boolean j2cl) {
        this.moduleName = moduleName;
        this.moduleShortName = moduleShortName;
        this.recompiler = recompiler;
        this.j2cl = j2cl;
        callbacks = Chain.startChain();
    }

    protected abstract GwtcJobMonitor getMonitor();

    @Override
    public void destroy() {

    }

    public CompileStatus getState() {
        return state;
    }

    public void setState(CompileStatus state) {
        try {
            if (state.isComplete()) {
                if (state == Failed) {
                    directory = null;
                    // TODO actually get ral error reporting here
                    error = new UnableToCompleteException();
                }
                callbacks.removeAll(callback-> {
                    try {
                        callback.in(directory, error);
                    } catch (Throwable e) {
                        callbackError = MultiException.mergedThrowable("Failure discovered in callback", e, callbackError);
                    }
                });
            }
            if (callbackError != null) {
                throw new Error(callbackError);
            }
        } finally {
            this.state = state;
        }
    }

    public void onDone(In2<CompiledDirectory, Throwable> callback) {
        if (state == Success) {
            callback.in(getDirectory(), null);
        } else {
            callbacks.add(callback);
            if (X_Process.isInProcess()) {
                // If we're already running in an X_Process thread, we can be free to block
                flushMonitor();
            } else {
                // We should not block on an unknown thread.  Do our blocking inside X_Process
                X_Process.runDeferred(this::flushMonitor);
            }
        }
    }

    private CompiledDirectory getDirectory() {
        flushMonitor();
        return directory;
    }

    protected synchronized void flushMonitor() {
        final GwtcJobMonitor monitor = getMonitor();
        // In order to effectively block until we've handled all new messages,
        // we will send a ping to the remote process, and then wait until we get
        // our ping id back; this ensures happens-before semantics with regard to
        // any pending operations
        while (monitor.hasMessageForCaller()) {
            readStatus(monitor, (status, extra)->{
                switch (status) {
                    case Log:
                        X_Log.info(GwtcJob.class, extra);
                        break;
                    case Success:
                        // We expect the extra bits to describe a CompiledDirectory
                        directory = recordResult(extra);
                    case Running:
                    case Failed:
                    case Preparing:
                        this.setState(status);

                }
            });
        }
    }

    protected void readStatus(GwtcJobMonitor monitor, In2<CompileStatus, String> callback) {

        String response = monitor.readAsCaller();
        if (response.isEmpty()) {
            X_Log.warn(GwtcJob.class, "Ignored empty response; did not call", callback);
            return;
        }
        final CompileStatus status = CompileStatus.fromChar(response.charAt(0));
        if (response.length() == 1) {
            X_Log.info(GwtcJob.class, "Received: ", status);
        } else {
            X_Log.info(GwtcJob.class, "Received: ", status, " : ", response);
        }
        String rest = response.substring(1);
            callback.in(status,
                (status == DebugWait ? "L" : "") +
                (rest.endsWith("\n") ? rest.substring(0, rest.length() - 1) : rest)
            );
    }


    private CompiledDirectory recordResult(String extra) {
        CompiledDirectory result = new CompiledDirectory();

        // TODO pass in a serializer to give user more control
        PrimitiveSerializer serializer = X_Inject.instance(PrimitiveSerializer.class);
        final CharIterator chars = CharIterator.forString(extra);
        String s;

        s = serializer.deserializeString(chars);
        result.setWarDir(s);

        s = serializer.deserializeString(chars);
        result.setWorkDir(s);

        s = serializer.deserializeString(chars);
        result.setDeployDir(s);

        s = serializer.deserializeString(chars);
        result.setExtraDir(s);

        s = serializer.deserializeString(chars);
        result.setGenDir(s);

        s = serializer.deserializeString(chars);
        result.setLogFile(s);

        s = serializer.deserializeString(chars);
        result.setSourceMapDir(s);

        s = serializer.deserializeString(chars);
        result.setStrategy(s == null ? null : CompileStrategy.valueOf(s));

        return result;
    }

    public boolean isRecompiler() {
        return recompiler;
    }

    public void forceRecompile() {
        // TODO implement me :-)
    }

    public void scheduleFlusher(GwtManifest manifest, Do afterShutdown) {
        assert manifest.isOnline() : "You must set GwtManifest.online = true";
        X_Process.runDeferred(()->{
            while (manifest.isOnline()) {
                try {
                    flushMonitor();
                } catch (Throwable t) {
                    X_Log.warn(GwtcJob.class, "Failed to flush remote process monitor; thread bailing", t);
                    manifest.setOnline(false);
                    afterShutdown.done();
                    throw t;
                }
            }
        });
    }

    public String[] toRecompileArgs(GwtManifest manifest) {

        Set<File> sourcePath = new LinkedHashSet<>();
        ChainBuilder<String> args = Chain.startChain();

        if (manifest.getUnitCacheDir() != null) {
            args.add(GwtcJobMonitor.ARG_UNIT_CACHE_DIR);
            args.add(manifest.getUnitCacheDir());
        }

        if (manifest.getLogFile() != null) {
            args.add(GwtcJobMonitor.ARG_LOG_FILE);
            args.add(manifest.getLogFile());
        }

        if (manifest.isRecompile()) {
            args.add(GwtcJobMonitor.JOB_RECOMPILE);
        } else if (manifest.isJ2cl()) {
            args.add(GwtcJobMonitor.JOB_J2CL);
        } else {
            args.add(GwtcJobMonitor.JOB_COMPILE);
        }

        for (String src : manifest.getSources().forEach()) {
            //TODO: sanitize this somehow?
            if (".".equals(src)) {
                src = new File("").getAbsolutePath();
            }
            if (src.startsWith("file:")) {
                src = src.substring(5);
            }
            File dir = new File(src);
            if (!dir.exists()) {
                if (src.startsWith("src")) {
                    final Class<?> main = X_Reflect.getMainClass();
                    if (main != null) {
                        String loc = X_Reflect.getSourceLoc(main);
                        if (loc != null) {
                            dir = new File(loc, src);
                        }
                        if (!dir.exists()) {
                            final URL sourceLoc = main.getProtectionDomain().getCodeSource().getLocation();
                            if (sourceLoc != null) {
                                // yay!
                                loc = sourceLoc.toString().replace("file:", "");
                                dir = new File(loc);
                                // TODO: cache / compress these computed fallbacks
                                if (dir.exists()) {
                                    int target = loc.lastIndexOf("/target");
                                    if (target != -1) {
                                        String base = loc.substring(0, target + 1);
                                        dir = new File(base, src);
                                    }
                                }
                            }
                        }
                    }
                    if (!dir.exists()) {
                        dir = new File(".", src);
                    }
                }
            }
            if (!dir.exists()) {
                X_Log.error(GwtcJobMonitor.class, "Gwt source directory " + dir + " does not exist");
            } else {
                X_Log.trace(GwtcJobMonitor.class, "Adding to source: " + dir);
            }
            sourcePath.add(dir);
        }
        for (String sources : manifest.getDependencies()) {
            for (String src : sources.split(File.pathSeparator)) {
                if (".".equals(src)) {
                    src = new File("").getAbsolutePath();
                }
                if (src.startsWith("file:")) {
                    src = src.substring(5);
                }
                File dir = new File(src);
                if (!dir.isAbsolute()) {
                    dir = new File(manifest.getRelativeRoot() + File.separator + dir.getPath());
                }
                if (dir.exists()) {
                    if (dir.isDirectory()) {
                        sourcePath.add(dir);
                        X_Log.trace(GwtcJobMonitor.class, "Adding to source path (will hot recompile): " + dir);
                    }
                } else {
                    final String error = "Gwt dependency directory " + dir + " does not exist";
                    X_Log.error(GwtcJob.class, error);
                    if (manifest.isStrict()) {
                        throw new IllegalStateException(error);
                    }
                }
            }

        }
        final File warDir = new File(manifest.getWarDir());
        if (!warDir.isDirectory()) {
            boolean result = warDir.mkdirs();
            if (!result) {
                X_Log.warn(GwtcJobMonitor.class, "Unable to create war dir", manifest.getWarDir(), "expect more errors...");
            }
        }

        if (manifest.isRecompile()) {
            if (!manifest.isStrict()) {
                args.add("-allowMissingSrc");
            }
        } else if (manifest.isStrict()) {
            args.add("-strict");
        }

        if (manifest.isRecompile()) {
            for (File file : sourcePath) {
                args.add("-src");
                try {
                    args.add(file.getCanonicalPath());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            if (!manifest.isPrecompile()) {
                args.add("-noprecompile");
            }
            if (manifest.getPort() != 0) {
                args.add("-port");
                args.add(Integer.toString(manifest.getPort()));
            }
        }


        if (manifest.getLogLevel() != null) {
            args.add("-logLevel");
            args.add(manifest.getLogLevel().getLabel());
        }

        if (manifest.getObfuscationLevel() != null) {
            args.add("-style");
            args.add(manifest.getObfuscationLevel().name());
        }

        if (manifest.getMethodNameMode() != null) {
            args.add("-XmethodNameDisplayMode");
            args.add(manifest.getMethodNameMode().name());
        }

        if (manifest.isIncremental()) {
            args.add("-incremental");
        } else {
            args.add("-noincremental");
        }

        args.add(manifest.getModuleName());

        final String[] fromManifest = manifest.toProgramArgArray();
        return args.toArray(String[]::new);
    }

}

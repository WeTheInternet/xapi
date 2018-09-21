package xapi.dev.gwtc.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.collect.impl.SimpleLinkedList;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage;
import xapi.except.MultiException;
import xapi.except.NoSuchItem;
import xapi.fu.*;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.model.api.PrimitiveSerializer;
import xapi.process.X_Process;
import xapi.reflect.X_Reflect;
import xapi.source.api.CharIterator;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_String;
import xapi.util.api.Destroyable;
import xapi.util.api.RemovalHandler;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage.*;
import static xapi.fu.Rethrowable.firstRethrowable;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.codeserver.CompileStrategy;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public abstract class GwtcJob implements Destroyable {

    private final String moduleName;
    private final String moduleShortName;
    protected final PrimitiveSerializer serializer;
    private CompileMessage state;
    private final boolean recompiler;
    private final boolean j2cl;
    private CompiledDirectory directory;
    private final ChainBuilder<In2<CompiledDirectory, Throwable>> doneCallbacks;
    private final SimpleLinkedList<In1<CompileMessage>> statusUpdate;
    private final StringTo.Many<In2<URL, Throwable>> resourceCallbacks;
    private Throwable error;
    private Throwable callbackError;
    private long touch;
    private volatile Do onStale = Do.NOTHING, onFresh = Do.NOTHING;

    public GwtcJob(GwtManifest manifest, PrimitiveSerializer serializer) {
        this(manifest.getModuleName(), manifest.getModuleShortName(), manifest.isRecompile(), manifest.isJ2cl(), serializer);
    }

    public GwtcJob(
        String moduleName,
        String moduleShortName,
        boolean recompiler,
        boolean j2cl,
        PrimitiveSerializer serializer
    ) {
        this.moduleName = moduleName;
        this.moduleShortName = moduleShortName;
        this.recompiler = recompiler;
        this.j2cl = j2cl;
        this.serializer = serializer;
        doneCallbacks = Chain.startChain();
        statusUpdate = new SimpleLinkedList<>();
        resourceCallbacks = X_Collect.newStringMultiMap(In2.class);
        touch();
    }

    private void touch() {
        touch = System.currentTimeMillis();
    }

    public abstract GwtcJobMonitor getMonitor();

    @Override
    public void destroy() {
        touch();
        if (getState() != CompileMessage.Destroyed) {
            X_Log.info(GwtcJob.class, "Telling gwtc process to die");
            getMonitor().writeAsCaller(GwtcJobMonitor.JOB_DIE);
            X_Process.scheduleInterruption(10, TimeUnit.SECONDS);
            while (getState() != Destroyed) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    flushMonitor();
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        X_Log.warn(GwtcJob.class, "Process did not die within 10s", e);
                    } else {
                        X_Log.error(GwtcJob.class, "Unknown error trying to kill gwtc job", e);
                    }
                    break;
                }
            }
        }
        doneCallbacks.removeAllUnsafe(callback-> {
            if (state == Success && directory != null) {
                callback.in(directory, null);
            } else {
                callback.in(null, new IllegalStateException("Job restarted before completing previous requests"));
            }
        });
    }

    public CompileMessage getState() {
        return state;
    }

    public void setState(CompileMessage state) {
        touch();
        try {
            if (state.isComplete()) {
                if (state == Failed || state == Destroyed) {
                    directory = null;
                    // TODO actually get real error reporting here
                    error = new UnableToCompleteException();
                    xapi.fu.log.Log.tryLog(GwtcJob.class, this, "Gwt Compilation state: ", state);
                }
                doneCallbacks.removeAll(callback-> {
                    try {
                        callback.in(directory, error);
                    } catch (Throwable e) {
                        callbackError = MultiException.mergedThrowable("Failure discovered in callback", e, callbackError);
                    }
                });
            }
            if (callbackError != null) {
                X_Fu.rethrow(callbackError);
            }
        } finally {
            callbackError = null;
            this.state = state;
            statusUpdate.forAll(In1::in, state);
        }
    }

    public RemovalHandler onStatusChange(In1<CompileMessage> callback) {
        if (state != null) {
            callback.in(state);
        }
        statusUpdate.add(callback);
        return ()->statusUpdate.remove(callback);

    }
    public void onDone(In2<CompiledDirectory, Throwable> callback) {
        if (state == Success) {
            callback.in(getDirectory(), null);
        } else {
            doneCallbacks.add(callback);
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

    public synchronized void flushMonitor() {
        final GwtcJobMonitor monitor = getMonitor();
        while (monitor.hasMessageForCaller()) {
            readStatus(monitor, (status, extra)->{
                touch();
                switch (status) {
                    case Log:
                        X_Log.info(GwtcJob.class, extra);
                        break;
                    case Success:
                        // We expect the extra bits to describe a CompiledDirectory
                        if (X_String.isNotEmptyTrimmed(extra)) {
                            // freshness checks will just return the status without the CompileDir
                            directory = recordResult(extra);
                        }
                    case Running:
                    case Failed:
                    case Preparing:
                    case DebugWait:
                    case Destroyed:
                        this.setState(status);
                        break;
                    case FoundResource:
                        notifyResource(extra);
                        break;
                    case Fresh:
                        notifyFresh();
                        break;
                    case Stale:
                        notifyStale();
                        break;
                }
            });
        }
    }

    private void notifyStale() {
        onStale.done();
    }

    private void notifyFresh() {
        onFresh.done();
    }

    private void notifyResource(String extra) {
        touch();
        CharIterator itr = CharIterator.forString(extra);
        String fileName = serializer.deserializeString(itr);
        String result = serializer.deserializeString(itr);
        synchronized (resourceCallbacks) {
            final IntTo<In2<URL, Throwable>> callbacks = resourceCallbacks.get(fileName);
            final URL url;
            if (X_String.isEmpty(result)) {
                url = null;
            } else {
                try {
                    url = new URL(result);
                } catch (MalformedURLException e) {
                    throw X_Fu.rethrow(e);
                }
            }
            callbacks.removeAll(callback->callback.in(url, url == null ? new NoSuchItem(extra) : null));
            resourceCallbacks.remove(fileName);
            resourceCallbacks.notifyAll();
        }
    }

    protected void readStatus(GwtcJobMonitor monitor, In2<CompileMessage, String> callback) {

        String response = monitor.readAsCaller();
        if (response.isEmpty()) {
            X_Log.warn(GwtcJob.class, "Ignored empty response; did not call", callback);
            return;
        }
        final CompileMessage status = CompileMessage.fromChar(response.charAt(0));
        if (response.length() == 1) {
            X_Log.trace(GwtcJob.class, "Received: ", status);
        } else {
            X_Log.trace(GwtcJob.class, "Received: ", status, " : ", response);
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

    public synchronized void forceRecompile() {
        if (state != Preparing && state != Running) {
            setState(Preparing);
            getMonitor().writeAsCaller(
                isRecompiler() ?
                GwtcJobMonitor.JOB_RECOMPILE :
                GwtcJobMonitor.JOB_COMPILE
            );
        }
    }

    public void scheduleFlusher(GwtManifest manifest, DoUnsafe afterShutdown) {
        assert manifest.isOnline() : "You must set GwtManifest.online = true";
        X_Process.runDeferred(()->{
            try {
                while (manifest.isOnline()) {
                    try {
                        flushMonitor();
                        synchronized (statusUpdate) {
                            try {
                                statusUpdate.wait(TimeUnit.SECONDS.toMillis(3));
                            } catch (InterruptedException ignored) { }
                        }
                    } catch (Throwable t) {
                        X_Log.warn(GwtcJob.class, "Failed to flush remote process monitor; thread bailing", t);
                        manifest.setOnline(false);
                        throw t;
                    }
                }
            } finally {
                afterShutdown.done();
            }
        });
    }

    public String[] toProgramArgs(GwtManifest manifest) {

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
                            final URL sourceLoc =
                                Maybe.nullable(main.getProtectionDomain())
                                        .mapNullSafe(ProtectionDomain::getCodeSource)
                                        .mapNullSafe(CodeSource::getLocation)
                                        .get();
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
        args.add("-war");
        args.add(warDir.getAbsolutePath());


        if (manifest.isRecompile()) {
            if (!manifest.isStrict()) {
                args.add("-allowMissingSrc");
            }
        } else {
            if (manifest.isStrict()) {
                args.add("-strict");
            }
            if (manifest.getGenDir() != null) {
                final File genDir = new File(manifest.getGenDir());
                if (!genDir.isDirectory()) {
                    boolean result = genDir.mkdirs();
                    if (!result) {
                        X_Log.warn(GwtcJobMonitor.class, "Unable to create gen dir", manifest.getWarDir(), "expect more errors...");
                    }
                }
                args.add("-gen");
                args.add(genDir.getAbsolutePath());
            }
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
        }

        if (manifest.isIncremental()) {
            args.add("-incremental");
        } else {
            args.add("-noincremental");
        }

        // Add all other arguments from the manifest (those above required special care)
        final String[] progArgs = manifest.toProgramArgArray();
        for (int i = 0; i < progArgs.length; i++) {
            switch (progArgs[i]) {
                case "-localWorkers":
                    if (isRecompiler()) {
                        i++;
                        continue;
                    }
                case "-war":
                case "-gen":
                case "-incremental":
                case "-noincremental":
                case "-src":
                case "-strict":
                case "-allowMissingSrc":
                    if (i < progArgs.length-1) {
                        if (!progArgs[i+1].startsWith("-")) {
                            i++;
                        }
                    }
                    continue;
            }
            args.add(progArgs[i]);
        }
        args.add(moduleName);
        return args.toArray(String[]::new);
    }

    public void blockFor(long timeout, TimeUnit unit) throws TimeoutException {
        final Moment start = X_Time.now();
        Moment deadline = start.plus(unit.toMillis(timeout));

        while (!isComplete(getState())) {
            if (getMonitor().hasMessageForCaller()) {
                flushMonitor();
            } else {
                LockSupport.parkNanos(50_000_000);
            }
            if (deadline.compareTo(X_Time.now()) < 0) {
                throw new TimeoutException("Waited " + timeout + " " + unit + " but job state was set to " + getState());
            }
        }
        while (getMonitor().hasMessageForCaller()) {
            flushMonitor();
        }
        X_Log.debug(GwtcJob.class, "Blocked for ", X_Time.difference(start));
    }

    /**
     * Request a given resource from a completed Gwt compilation.
     *
     *
     * @param fileName - The java-classpath name of the resource to load
     * @param callback - The callback to notify upon completion
     * @return - A DoUnsafe which will block until the callback has been called.
     */
    public DoUnsafe requestResource(String fileName, In2Unsafe<URL, Throwable> callback) {
        synchronized (resourceCallbacks) {
            resourceCallbacks.add(fileName, callback);
        }
        getMonitor().writeAsCaller(
            GwtcJobMonitor.JOB_GET_RESOURCE + fileName
        );
        return ()-> {
            final IntTo<In2<URL, Throwable>> callbacks = resourceCallbacks.get(fileName);
            while (callbacks.contains(callback)) {
                flushMonitor();
                if (callbacks.contains(callback)) {
                    synchronized (resourceCallbacks) {
                        resourceCallbacks.wait(100);
                    }
                }
            }
        };
    }

    public long lastChange() {
        return touch;
    }

    public String getModuleName() {
        return moduleName;
    }

    final AtomicBoolean result = new AtomicBoolean();
    public boolean checkFreshness() {
        synchronized (result) {
            boolean first = onFresh == Do.NOTHING;

            X_Log.debug(GwtcJob.class, "checking freshness... ", first);
            if (first) {
                onFresh = ()->{
                    result.set(true);
                    synchronized (result) {
                        result.notifyAll();
                        onFresh = onStale = Do.NOTHING;
                    }
                };
                onStale = ()->{
                    result.set(false);
                    synchronized (result) {
                        result.notifyAll();
                        onFresh = onStale = Do.NOTHING;
                    }
                };
                getMonitor().writeAsCaller(GwtcJobMonitor.JOB_CHECK_FRESHNESS);
            }
        }

        int spins = 100;
        try {
            while (onFresh != Do.NOTHING) {
                pingMonitor();
                if (spins --< 0) {
                    synchronized (result) {
                        result.wait(10);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            firstRethrowable(this).rethrow(e);
        }

        return result.get();
    }

    public void pingMonitor() {
        synchronized (statusUpdate) {
            statusUpdate.notifyAll();
        }
    }
}

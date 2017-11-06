package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJobState;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.gwtc.api.IsAppSpace;
import xapi.fu.Immutable;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.In2Out1;
import xapi.fu.Lazy;
import xapi.fu.Mutable;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.IsRecompiler;
import xapi.gwtc.api.ServerRecompiler;
import xapi.log.X_Log;
import xapi.time.X_Time;

import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.codeserver.JobEvent;
import com.google.gwt.dev.codeserver.JobEvent.Status;
import com.google.gwt.dev.codeserver.RecompileController;
import com.google.gwt.dev.codeserver.GwtcEventTable;
import com.google.gwt.dev.codeserver.SuperDevUtil;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/20/17.
 */
public class GwtcJobStateImpl implements GwtcJobState {

    private GwtManifest manifest;
    private final ChainBuilder<In1<IsRecompiler>> runOnCaller;
    private String argsOnStart;
    private In2Out1<Integer, TimeUnit, Integer> blocker;
    private Status status;

    private final GwtcService service;
    private final Lazy<RecompileController> compiler;
    private final Lazy<TreeLogger> logger;
//    private Lazy<String> gwtHome;
    private Lazy<IsAppSpace> appSpace;
    private Lazy<URLClassLoader> classloader;
    private Lazy<In1<Integer>> shutdown;
    private Lazy<ServerRecompiler> api;
    private volatile String currentEventId;
    private final String moduleName;
    private final String moduleShortName;

    public GwtcJobStateImpl(GwtManifest manifest, GwtcService gwtcService) {
        this.moduleName = manifest.getModuleName();
        this.moduleShortName = manifest.getModuleShortName();
        this.manifest = manifest;
        this.service = gwtcService;
        status = Status.WAITING;
        runOnCaller = Chain.startChain();
        logger = Lazy.deferred1(this::createLogger);
        compiler = Lazy.deferred1(this::createController);
        // TODO: make these lazies part of a "resettable group", so when we redo one, we redo all...
        appSpace = initAppSpace();
        classloader = initClassLoader();
        shutdown = initShutdown();
        api = initApi();
    }

    private Lazy<IsAppSpace> initAppSpace() {
        return Lazy.deferBoth(SuperDevUtil::newAppSpace, this::getModuleName);
    }

    private Lazy<ServerRecompiler> initApi() {
        // We're creating a lazy supplier of a factory which accepts callbacks for our controller
        return Lazy.deferred1(()->callback->{
            // This code is run whenever calling ServerRecompiler.useServer()
            if (isSuccess()) {
                compiler.out1().checkFreshness(
                    callback.provide(this::getController),
                    ()->{
                        runOnCaller.add(callback);
                        synchronized (runOnCaller) {
                            X_Log.info(GwtcJobStateImpl.class,
                                "Notify runOnCaller");
                            runOnCaller.notifyAll();
                        }
                        // now wait for the callback to be notified, so we run callback from correct classloader...
                        synchronized (callback) {
                            try {
                                X_Log.info(GwtcJobStateImpl.class,
                                    "Waiting for callback", callback);
                                callback.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        }
                        callback.in(compiler.out1());
                    }
                );
            } else {
                runOnCaller.add(callback);
            }
        });
    }

    private Lazy<In1<Integer>> initShutdown() {
        return Lazy.deferred1(service::prepareCleanup, manifest);
    }

    private Lazy<URLClassLoader> initClassLoader() {
        return Lazy.deferred1(()-> {
            service.generateCompile(manifest);
            return service.resolveClasspath(manifest);
        });
    }


    protected RecompileController createController() {
        final RecompileController controller = SuperDevUtil.getOrMakeController(
            this,
            manifest,
            logger.out1()
        );
        final GwtcEventTable table = controller.getRunner().getTable();
        final String name = manifest.getModuleName();
        table.listenForEvents(name, Status.COMPILING, false, ev->{
            currentEventId = ev.getJobId();
            status = ev.getStatus();
        });
        table.listenForEvents(name, Status.SERVING, false, ev->{
            if (ev.getJobId().equals(currentEventId)) {
                X_Time.runLater(()->{
                    // We defer here to ensure that the calling code has time to wait.
                    // it would be exceptionally hard to hit this race condition,
                    // but it is theoretically possible if the calling thread is frozen for a long time
                    // (for example, in a debugger with only certain threads paused).
                    runOnCaller.removeAll(callback->{
                        X_Log.info(GwtcJobStateImpl.class, "Notify callbacks gwt compile completed for", callback);
                        synchronized (callback) {
                            callback.notifyAll();
                        }
                    });
                });
            }
        });
        table.listenForEvents(name, null, false, ev->{
            X_Log.info(GwtcJobStateImpl.class, "Gwt Event", ev.getJobId(), "status:", ev.getStatus());
            if (ev.getJobId().equals(currentEventId)){
                X_Log.info(GwtcJobStateImpl.class, "Setting job", ev.getJobId(), "status to ", ev.getStatus());
                status = ev.getStatus();
            }
        });

        return controller;
    }

    private PrintWriterTreeLogger createLogger() {
        final PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
        logger.setMaxDetail(manifest.getLogLevel());
        return logger;
    }

    public GwtcJobStateImpl setBlocker(In2Unsafe<Integer, TimeUnit> blocker, Mutable<Integer> code) {
        this.blocker = blocker.supply1AfterRead(code);
        return this;
    }

    @Override
    public boolean isRunning() {
        if (status == Status.COMPILING) {
            return true;
        }
        if (compiler.isUnresolved()) {
            return false;
        }
        final GwtcEventTable table = eventTable();
        final JobEvent status = table.getCompilingJobEvent();
        if (status == null) {
            return false;
        }
        switch (status.getStatus()) {
            case COMPILING:
            case WAITING:
                return true;
        }
        return false;
    }

    @Override
    public boolean isFailed() {
        if (status == Status.ERROR) {
            return true;
        }
        if (compiler.isUnresolved()) {
            return false;
        }
        final GwtcEventTable table = eventTable();
        final JobEvent status = table.getCompilingJobEvent();
        if (status == null) {
            return false;
        }
        return status.getStatus() == Status.ERROR;
    }

    @Override
    public boolean isSuccess() {
        if (status == Status.SERVING) {
            return true;
        }
        if (compiler.isUnresolved()) {
            return false;
        }
        final GwtcEventTable table = eventTable();
        final JobEvent status = table.getCompilingJobEvent();
        if (status == null) {
            return false;
        }
        return status.getStatus() == Status.SERVING;
    }

    private GwtcEventTable eventTable() {
        return compiler.out1().getRunner().getTable();
    }

    @Override
    public In2Out1<Integer, TimeUnit, Integer> getBlocker() {
        return blocker;
    }

    @Override
    public void destroy() {
        if (compiler.isResolved()) {
            compiler.out1().cleanup();
        }
        classloader = initClassLoader();
        // TODO: actually use shutdown if we haven't already shutdown;
        // for now, ignoring this state management as there are bigger fish to fry
        shutdown = initShutdown();

    }

    @Override
    public void onStart(GwtManifest manifest) {
        this.manifest = manifest;
        this.argsOnStart = manifest.toProgramArgs();
    }

    @Override
    public boolean isReusable(GwtManifest manifest) {
        if (isFailed()) {
            return false;
        }
        if (this.manifest == null) {
            return true;
        } else {
            // For now, we'll consider the manifest reusable if args have not changed.
            // We should have a further means of invalidating compilations (say, using FileWatcher, etc.)
            final String newArgs = manifest.toProgramArgs();
            return newArgs.equals(argsOnStart);
        }
    }

    public RecompileController getController() {
        return compiler.out1();
    }

    public void startCompile(In2<ServerRecompiler, Throwable> callback) {

        if (!isReusable(manifest)) {
            destroy();
        }
        if (isSuccess()) {
            // The job is still running; queue up this callback and carry on...
            // TODO: check manifest / source files for changes and queue a recompile

            callback.in(api.out1(), null);
            return;
        }
        if (isRunning()) {
            final Mutable<IsRecompiler> t = new Mutable<>();
            runOnCaller.add(recomp-> {
                t.in(recomp);
                synchronized (t) {
                    t.notifyAll();
                }
            });
            getBlocker().io(300, TimeUnit.SECONDS);
            final IsRecompiler recomp = t.out1();
            // TODO: reflection-backed delegate...

            callback.in(d->d.in(recomp), null);
            return;
        }

        // Start the job
        onStart(manifest);

        final URLClassLoader loader = classloader.out1();

        Mutable<Integer> code = new Mutable<>();
        final Runnable task = () -> {
            manifest.setOnline(true);
            boolean called = false;
            try {
                while ( manifest.isOnline() ) {
                    called = false;
                    final RecompileController compiler = getController();

                    final CompiledDirectory result = compiler.recompile();

                    manifest.setCompileDirectory(result);

                    code.in(0);
                    // We are pre-emptively cleaning up here... might want to put this somewhere more... managed.
                    // cleanup.in(0);
                    called = true;
                    callback.in(api.out1(), null);
                    while (runOnCaller.isEmpty()) {
                        synchronized (runOnCaller) {
                            try {
                                runOnCaller.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }
                }
            } catch (Throwable fail) {
                manifest.setOnline(false);
                if (!called) {
                    callback.in(null, fail);
                }
                shutdown.out1().in(-1);
                X_Log.error(GwtcJobStateImpl.class, "Manifest no longer online due to error;",
                    fail, "quitting compiler", manifest.getModuleName());
                throw fail;
            }
            X_Log.info(GwtcJobStateImpl.class, "Manifest no longer online; quitting compiler", manifest.getModuleName());
        };
        In2Unsafe<Integer, TimeUnit> blocker = service.startTask(task, loader);

        setBlocker(blocker,  code);
    }

    @Override
    public IsAppSpace getAppSpace() {
        return appSpace.out1();
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getModuleShortName() {
        return moduleShortName;
    }
}

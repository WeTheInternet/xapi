package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.impl.ExtensibleClassLoader;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.util.X_Util;
import xapi.util.api.Destroyable;

import java.net.URLClassLoader;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/28/17.
 */
public class IsolatedCompiler implements Runnable, Destroyable {

    private final String[] args;
    private final GwtcJobMonitor remoteMonitor;
    private GwtcJobManagerImpl manager;

    public IsolatedCompiler(Object remoteMonitor, String[] args) {
        this.remoteMonitor = (GwtcJobMonitor) remoteMonitor;
        this.args = args;
    }

    @Override
    public void run() {
        try {
            GwtcService compiler = X_Inject.instance(GwtcService.class);
            manager = new GwtcJobManagerImpl(compiler);
            manager.runJob(remoteMonitor, args);
        } catch (Throwable t) {
            final Throwable unwrapped = X_Util.unwrap(t);
            if (unwrapped instanceof ClassNotFoundException || unwrapped instanceof LinkageError) {
                // Our classloader is borked (underlying jars likely changed)
                // Lets launch a new thread with a new / fresh classloader.
                X_Log.warn(IsolatedCompiler.class, "Isolated compiler detected class loading error", t,
                    "refreshing classloader in a new thread.");
                URLClassLoader cl = ExtensibleClassLoader.cloneLoader(Thread.currentThread().getContextClassLoader());
                IsolatedCompiler oneUp = new IsolatedCompiler(remoteMonitor, args);
                X_Process.runInClassloader(cl, oneUp::run);
                manager.killJobs();
                return;
            }
            X_Log.error(IsolatedCompiler.class, "Isolated compiler failed", t);
            throw t;
        }
    }

    @Override
    public void destroy() {
        X_Log.info(IsolatedCompiler.class, "Destroying manager");
        if (manager != null) {
            manager.die(remoteMonitor);
        }
    }
}

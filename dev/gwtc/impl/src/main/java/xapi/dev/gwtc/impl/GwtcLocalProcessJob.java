package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.fu.Do;
import xapi.gwtc.api.GwtManifest;
import xapi.process.X_Process;
import xapi.reflect.X_Reflect;
import xapi.util.X_Debug;

import java.net.URLClassLoader;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A Gwtc job that runs in the current JVM.  It should be launched in a (preferable) isolated classloader,
 * and will communicate across the classworlds using a reflection-based {@link GwtcJobMonitorImpl}
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public class GwtcLocalProcessJob extends GwtcJob {

    private final GwtcJobMonitorImpl monitor;
    private GwtcJobManagerImpl manager;

    public GwtcLocalProcessJob(GwtManifest manifest, URLClassLoader classpath, String gwtHome) {
        super(manifest);
        final LinkedBlockingDeque<String> callerDeque = new LinkedBlockingDeque<>();
        final LinkedBlockingDeque<String> compilerDeque = new LinkedBlockingDeque<>();
        this.monitor = new GwtcJobMonitorImpl(callerDeque, compilerDeque);

        final String[] args = toRecompileArgs(manifest);

        Object remote = monitor.forClassloader(classpath, callerDeque, compilerDeque);
        Runnable task;
        try {
            task = (Runnable)X_Reflect.construct(
                classpath.loadClass(IsolatedCompiler.class.getName()),
                new Class[]{Object.class, String[].class}, remote, args
            );
        } catch (Throwable throwable) {
            throw X_Debug.rethrow(throwable);
        }
        manifest.setOnline(true);
        X_Process.runInClassloader(classpath, task::run);
        scheduleFlusher(manifest, Do.NOTHING);
    }

    @Override
    public GwtcJobMonitor getMonitor() {
        return monitor;
    }
}

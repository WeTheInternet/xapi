package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage;
import xapi.gwtc.api.GwtManifest;
import xapi.model.api.PrimitiveSerializer;
import xapi.process.X_Process;
import xapi.reflect.X_Reflect;
import xapi.debug.X_Debug;

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

    public GwtcLocalProcessJob(
        GwtManifest manifest,
        URLClassLoader classpath,
        PrimitiveSerializer serializer
    ) {
        super(manifest, serializer);
        final LinkedBlockingDeque<String> callerDeque = new LinkedBlockingDeque<>();
        final LinkedBlockingDeque<String> compilerDeque = new LinkedBlockingDeque<>();
        this.monitor = new GwtcJobMonitorImpl(callerDeque, compilerDeque);

        if (manifest.getLogFile() == null) {
            manifest.setLogFile(GwtcJobMonitor.NO_LOG_FILE);
        }
        final String[] args = toProgramArgs(manifest);

        Object remote = monitor.forClassloader(classpath, callerDeque, compilerDeque);
        Runnable task;
        try {
            // We use Runnable here because it is loaded by bootstrap classloader.
            // thus, we can more freely share them across classloaders
            task = (Runnable)X_Reflect.construct(
                classpath.loadClass(IsolatedCompiler.class.getName()),
                new Class[]{Object.class, String[].class}, remote, args
            );
        } catch (Throwable throwable) {
            throw X_Debug.rethrow(throwable);
        }
        manifest.setOnline(true);
        X_Process.runInClassloader(classpath, ()->{
            try {
                task.run();
            } catch (Throwable t) {
                monitor.updateCompileStatus(CompileMessage.Failed, "Failed on " + t);
            }
        });
        scheduleFlusher(manifest, ()->
            task.getClass().getMethod("destroy")
                .invoke(task)
        );
    }

    @Override
    public GwtcJobMonitor getMonitor() {
        return monitor;
    }
}

package xapi.dev.gwtc.api;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileStatus;
import xapi.fu.In2;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

/**
 * This class is the successor to the previously fragmented {@link GwtcService} and {@link GwtcJobState} and {@link com.google.gwt.dev.codeserver.RecompileRunner}.
 *
 * The purpose of this class is to receive compile requests from a {@link GwtManifest} parameter,
 * generate the correct files and classpath, instantiate a runtime environment (classloader or process),
 * then monitor that environment for status and/or send it instructions / requests.
 *
 * This allows us to keep multiple compilations alive and servicing requests,
 * with guarantees about classpath (and ugly static var) isolation.
 *
 * The Gwt compiler leaks some static state, so running more than one
 * compilation on a shared classloader with different dependencies is an unsupported configuration.
 *
 * That is to say, it will probably work, most of the time... So you should still prefer an isolated classloader
 * (no shared parent / different static universe), or running as a separate process entirely
 * (necessary if using google's hack for getting java 9 classpaths: reading System.getProperty("java.class.path")).
 *
 * Users of XApi fork + Java 9 should be able to avoid this by properly specifying open modules,
 * either by creating modules opened to the gwt module, or by passing flags to jvm to allow reading classloaders.
 *
 * Users of java 8 have nothing to worry about. :-)
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public abstract class GwtcJobManager {

    protected final StringTo<GwtcJob> runningJobs;
    protected final StringTo.Many<In2<CompiledDirectory, Throwable>> callbacks;
    protected final GwtcService service;

    public GwtcJobManager(GwtcService service) {
        this.runningJobs = X_Collect.newStringMap(GwtcJob.class);
        this.callbacks = X_Collect.newStringMultiMap(In2.class);
        this.service = service;
    }

    public void compileIfNecessary(GwtManifest manifest, In2<CompiledDirectory, Throwable> callback) {

        // This is going to be the new primary compilation method for all Gwt compiles;
        // all others will be deprecated once this one is complete and tested.

        // We want this method to correctly handle foreign classloading;
        // That is, we want to stash our callbacks, create and call into a Gwt environment,
        // and then have the same thread that called this method handle the callback.
        // This is necessary for us to have "clean room" classloaders for gwt compilation,
        // where the server doesn't leak into gwt, and gwt doesn't leak into server.

        String name = manifest.getModuleName();
        GwtcJob running = runningJobs.compute(name,
            existing -> {
                // Job already existed; check if it is stale or not
                if (existing.getState() == CompileStatus.Failed || looksBroken(manifest, existing)) {
                    existing.destroy();
                    runningJobs.remove(name);
                    runningJobs.put(name, launchJob(manifest));
                }
            },
            ()-> launchJob(manifest)
        );
        running.onDone(callback);
    }

    protected boolean looksBroken(GwtManifest manifest, GwtcJob existing) {
        return false;
    }

    protected abstract GwtcJob launchJob(GwtManifest manifest);

    public void forceRecompile(GwtManifest manifest, In2<CompiledDirectory, Throwable> callback) {
        String name = manifest.getModuleName();
        GwtcJob running = runningJobs.remove(name);
        if (running != null) {
            if (running.isRecompiler()) {
                running.forceRecompile();
                running.onDone(callback);
            } else {
                running.destroy();
            }
        }
        compileIfNecessary(manifest, callback);
    }

    public void blockFor(String moduleName, long timeout, TimeUnit unit) throws TimeoutException {
        final GwtcJob job = runningJobs.get(moduleName);
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);

        while (!GwtcJobState.isComplete(job.getState())) {
            if (job.getMonitor().hasMessageForCaller()) {
                job.flushMonitor();
            } else {
                LockSupport.parkNanos(10_000_000);
            }
            if (System.currentTimeMillis() > deadline) {
                throw new TimeoutException("Waited " + timeout + " " + unit + " but job state was set to " + job.getState());
            }
        }
        while (job.getMonitor().hasMessageForCaller()) {
            job.flushMonitor();
        }
    }
}

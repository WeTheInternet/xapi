package xapi.dev.gwtc.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.gwtc.api.*;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage;
import xapi.fu.In2;
import xapi.fu.In3;
import xapi.fu.Mutable;
import xapi.fu.Out2;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.PrimitiveSerializerDefault;
import xapi.process.X_Process;
import xapi.source.api.CharIterator;
import xapi.time.X_Time;
import xapi.time.api.Moment;

import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static xapi.time.X_Time.diff;

import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.codeserver.CompileStrategy;

/**
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
public abstract class GwtcJobManagerAbstract implements GwtcJobManager {

    class StaleJar extends Error {}

    protected final StringTo<GwtcJob> runningJobs;
    protected final StringTo<CompileMessage> statuses;
    protected final StringTo.Many<In2<CompiledDirectory, Throwable>> callbacks;
    protected final GwtcService service;
    protected final PrimitiveSerializer serializer;

    public GwtcJobManagerAbstract(GwtcService service) {
        this.runningJobs = X_Collect.newStringMap(GwtcJob.class);
        this.statuses = X_Collect.newStringMap(CompileMessage.class);
        this.callbacks = X_Collect.newStringMultiMap(In2.class);
        this.service = service;
        this.serializer = newSerializer();
    }

    protected PrimitiveSerializer newSerializer() {
        return new PrimitiveSerializerDefault();
    }

    @Override
    public void compileIfNecessary(GwtManifest manifest, In2<CompiledDirectory, Throwable> callback) {

        // This is going to be the new primary compilation method for all Gwt compiles;
        // all others will be deprecated once this one is complete and tested.

        final Moment start = X_Time.now();

        // We want this method to correctly handle foreign classloading;
        // That is, we want to stash our callbacks, create and call into a Gwt environment,
        // and then have the same thread that called this method handle the callback.
        // This is necessary for us to have "clean room" classloaders for gwt compilation,
        // where the server doesn't leak into gwt, and gwt doesn't leak into server.

        String name = manifest.getModuleName();
        boolean[] isNew = {false};
        GwtcJob existing = runningJobs.getOrCreate(name,
            n-> {
                isNew[0] = true;
                X_Log.info(GwtcJobManagerAbstract.class, "Starting compilation for ", manifest.getModuleName());
                return launchJob(manifest);
            }
        );
        final In2<CompiledDirectory, Throwable> whenDone = callback.doBeforeMe((dir, fail)->{
            X_Log.trace(GwtcJobManagerAbstract.class, "Gwtc job ", (fail == null ? "succeeded" : "failed"), "after waiting", diff(start));
            if (dir != null) {
                manifest.setCompileDirectory(dir);
            }
        });
        if (!isNew[0]) {
            // when we aren't new, we'll want to kick off some initialization.
            if (existing.getState() == CompileMessage.Failed || existing.getState() == CompileMessage.Destroyed) {
                if (runningJobs.containsKey(name)) {
                    X_Log.info(GwtcJobManagerAbstract.class, "Restarting ", existing.getState(), " compilation for ", manifest.getModuleName());
                    // Kill the job through the gwtc service (calls into our own destroy() method)
                    service.destroy(existing);
                    // we may need to also "destroy" our classloader, or at least restart a thread from a sane copy...
                    // killing the job should suffice; if not, we should use more-draconian gwt compile classloader / process isolation.
                    assert !runningJobs.containsKey(name) : "GwtcService " + service + " did not call getJobManager().destroy(job);";
                    statuses.put(name, CompileMessage.Preparing);
                    final GwtcJob newJob = launchJob(manifest);
                    newJob.onDone(whenDone);
                    runningJobs.put(name, newJob);
                }
            } else {
                maybeRecompile(manifest, existing);
                X_Log.debug(GwtcJobManagerAbstract.class, "Sent recompile request in ", diff(start));
            }
        }
        existing.onDone(whenDone);
    }

    @SuppressWarnings("Duplicates")
    protected boolean maybeRecompile(GwtManifest manifest, GwtcJob existing) {
        final Moment start = X_Time.now();
        if (existing.getState() == CompileMessage.Success) {
            // Needs to run inside the job, so we can talk to the correct classloader...
            if (existing.checkFreshness()) {
                return true;
            }
        }
        Mutable<CompileMessage> status = new Mutable<>();
        existing.onStatusChange(s->{
            status.in(s);
            synchronized (status) {
                status.notifyAll();
            }
        });
        final Moment blockingStarted = X_Time.now();
        existing.forceRecompile();
        final Long limit = manifest.getMaxCompileMillis();
        X_Log.trace(GwtcJobManagerAbstract.class, "Finished recompilation check for module", manifest.getModuleName(),
            "in ", X_Time.diff(blockingStarted));
        if (limit != null) {
            X_Process.scheduleInterruption(limit, TimeUnit.MILLISECONDS);
        }
        while (!existing.getState().isComplete()) {
            try {
                if (existing.getMonitor().hasMessageForCaller()) {
                    existing.flushMonitor();
                } else {
                    synchronized (status) {
                        status.wait(250);
                    }
                }
            } catch (InterruptedException doneHere) {
                break;
            }
        }
        if (status.out1() == CompileMessage.Success) {
            X_Log.info(GwtcJobManagerAbstract.class, "Module ", manifest.getModuleName(),
                "compilation succeeded in ", diff(start));
            return false;
        }

        final double waited = X_Time.now().millis() - blockingStarted.millis();
        if (limit != null) {
            if (waited < limit) {
                X_Log.warn(GwtcJobManagerAbstract.class, "Thread interrupted at unexpected time");
            } else {
                X_Log.warn(GwtcJobManagerAbstract.class, "Compilation aborted after ",
                    diff(blockingStarted), "for module", manifest.getModuleName()
                );
            }
        }
        return false;
    }

    protected abstract GwtcJob launchJob(GwtManifest manifest);

    @Override
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

    @Override
    public void blockFor(String moduleName, long timeout, TimeUnit unit) throws TimeoutException {
        final GwtcJob job = runningJobs.get(moduleName);
        job.blockFor(timeout, unit);
    }

    protected void sendCompilerResults(
        GwtcJobMonitor monitor,
        String warDir,
        String workDir,
        String deployDir,
        String extraDir,
        String genDir,
        String logFile,
        String sourceDir,
        SortedSet<BindingProperty> bindings,
        CompileStrategy strategy
    ) {
        // TODO: somehow get a sane Strongname mapping from binding props, and send them too.
        if (strategy == CompileStrategy.SKIPPED) {
            // when recompiler skipped its work, we don't want to return any new CompileDir.
            // the job listener will see this and reuse the files it already knows about
            monitor.updateCompileStatus(CompileMessage.Success);
            return;
        }
        monitor.updateCompileStatus(
            CompileMessage.Success,
            // We use our primitive serializer because it writes string length, then contents,
            // such that we can simply read the same number of strings on the other end,
            // to construct a resulting CompiledDirectory for consumption by registered callbacks.
            serializer.serializeString(warDir)
            ,serializer.serializeString(workDir)
            ,serializer.serializeString(deployDir)
            ,serializer.serializeString(extraDir)
            ,serializer.serializeString(genDir)
            ,serializer.serializeString(logFile)
            ,serializer.serializeString(sourceDir)
            ,serializer.serializeString((strategy == null ? CompileStrategy.FULL : strategy).name())
        );

    }

    protected CompiledDirectory deserialize(String str) {
        CompiledDirectory dir = initCompileDirectory();
        final CharIterator chars = CharIterator.forString(str);
        dir.setWarDir(serializer.deserializeString(chars));
        dir.setDeployDir(serializer.deserializeString(chars));
        dir.setExtraDir(serializer.deserializeString(chars));
        dir.setGenDir(serializer.deserializeString(chars));
        dir.setLogFile(serializer.deserializeString(chars));
        dir.setSourceMapDir(serializer.deserializeString(chars));
        dir.setStrategy(CompileStrategy.valueOf(serializer.deserializeString(chars)));
        return dir;
    }

    protected CompiledDirectory initCompileDirectory() {
        return new CompiledDirectory();
    }

    @Override
    public CompileMessage getStatus(String moduleName) {
        final GwtcJob job = getJob(moduleName);
        if (job != null) {
            return job.getState();
        }
        return statuses.getOrReturn(moduleName, CompileMessage.Destroyed);
    }

    @Override
    public GwtcJob getJob(String moduleName) {
        return runningJobs.get(moduleName);
    }

    public void killJobs() {
        for (Out2<String, GwtcJob> item : runningJobs.removeAllItems()) {
            item.out2().destroy();
        }
    }

    @Override
    public void destroy(GwtcJob existing) {
        final IntTo<In2<CompiledDirectory, Throwable>> removed = callbacks.remove(existing.getModuleName());
        if (removed != null) {
            removed.forAll(In3.invokeIn2(), (CompiledDirectory)null, new JobCanceledException());
        }
        runningJobs.remove(existing.getModuleName());
        statuses.remove(existing.getModuleName());
        existing.destroy();
    }
}

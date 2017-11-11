package xapi.dev.gwtc.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobManager;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage;
import xapi.dev.gwtc.api.GwtcJobState;
import xapi.dev.gwtc.api.GwtcService;
import xapi.fu.In2;
import xapi.fu.Mutable;
import xapi.fu.X_Fu;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.PrimitiveSerializerDefault;
import xapi.source.api.CharIterator;

import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.codeserver.CompileStrategy;
import com.google.gwt.dev.resource.impl.ResourceAccumulatorManager;

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
public abstract class GwtcJobManagerAbstract implements GwtcJobManager {

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
                return launchJob(manifest);
            }
        );
        if (!isNew[0]) {
            // when we aren't new, we'll want to kick off some initialization.
            if (existing.getState() == CompileMessage.Failed || existing.getState() == CompileMessage.Destroyed) {
                existing.destroy();
                runningJobs.remove(name);
                statuses.put(name, CompileMessage.Preparing);
                runningJobs.put(name, launchJob(manifest));
            } else {
                maybeRecompile(manifest, existing);
            }
        }
        existing.onDone(callback.doBeforeMe((dir, fail)->{
            if (dir != null) {
                manifest.setCompileDirectory(dir);
            }
        }));
    }

    @SuppressWarnings("Duplicates")
    protected synchronized boolean maybeRecompile(GwtManifest manifest, GwtcJob existing) {
        if (existing.getState() == null) {
            return true;
        }
        if (existing.getState() == CompileMessage.Success) {
            // now we also need to check freshness...
            Mutable<Boolean> result = new Mutable<>();
            ResourceAccumulatorManager.checkCompileFreshness(
                ()->{
                    // when fresh, maybeRecompile returns false
                    result.in(false);
                    synchronized (result) {
                        result.notifyAll();
                    }
                },
                ()->{
                    result.in(true);
                    synchronized (result) {
                        result.notifyAll();
                    }
                }
            );
            synchronized (result) {
                try {
                    if (result.isNull()) {
                        result.wait();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw X_Fu.rethrow(e);
                }
            }
            if (!result.out1()) {
                return false;
            }
        }
        Mutable<CompileMessage> status = new Mutable<>();
        existing.onStatusChange(s->{
            status.in(s);
            synchronized (status) {
                status.notifyAll();
            }
        });
        existing.forceRecompile();
        while (!existing.getState().isComplete()) {
            try {
                if (existing.getMonitor().hasMessageForCaller()) {
                    existing.flushMonitor();
                } else {
                    synchronized (status) {
                        status.wait(250);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw X_Fu.rethrow(e);
            }
        }

        return status.out1() != CompileMessage.Success;
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

}

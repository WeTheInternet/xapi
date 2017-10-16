package xapi.dev.gwtc.api;

import xapi.dev.gwtc.api.GwtcJobMonitor.CompileStatus;
import xapi.fu.In2;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.util.api.Destroyable;

import java.util.concurrent.CountDownLatch;

import static xapi.dev.gwtc.api.GwtcJobMonitor.CompileStatus.Success;

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
    private volatile int pingId;

    public GwtcJob(GwtManifest manifest) {
        this(manifest.getModuleName(), manifest.getModuleShortName(), manifest.isRecompile(), manifest.isJ2cl());
    }

    public GwtcJob(String moduleName, String moduleShortName, boolean recompiler, boolean j2cl) {
        this.moduleName = moduleName;
        this.moduleShortName = moduleShortName;
        this.recompiler = recompiler;
        this.j2cl = j2cl;
    }

    protected abstract GwtcJobMonitor getMonitor();

    @Override
    public void destroy() {

    }

    public CompileStatus getState() {
        return state;
    }

    public void setState(CompileStatus state) {
        this.state = state;
    }

    public void onDone(In2<CompiledDirectory, Throwable> callback) {
        if (state == Success) {
            callback.in(getDirectory(), null);
        } else {
            if (X_Process.isInProcess()) {
                // If we're already running in an X_Process thread, we can be free to block
                flushMonitor();
            } else {
                // We should not block on an unknown thread.  Do our blocking inside X_Process
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
        final String ident = Integer.toString(pingId++, 36);
        monitor.ping(ident);
        CountDownLatch latch = new CountDownLatch(1);
        while (latch.getCount() > 0) {
            monitor.readStatus((status, extra)->{
                switch (status) {
                    case Log:
                        X_Log.info(GwtcJob.class, extra);
                        break;
                    case Ping:
                        if (ident.equals(extra)) {
                            // alright, our monitor flush is done!
                            latch.countDown();
                        }
                        return;
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
        try {
            latch.await();
        } catch (InterruptedException e) {
            // We've been interrupted.  Time to die.
            X_Log.warn(GwtcJob.class, "Interrupted; Killing compiler", e);
            destroy();
        }
    }

    private CompiledDirectory recordResult(String extra) {

        return null;
    }

    public boolean isRecompiler() {
        return recompiler;
    }

    public void forceRecompile() {

    }

}

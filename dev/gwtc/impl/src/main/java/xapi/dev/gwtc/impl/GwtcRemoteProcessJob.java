package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileStatus;
import xapi.gwtc.api.GwtManifest;
import xapi.io.api.LineReader;
import xapi.io.api.SimpleLineReader;
import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.shell.api.ShellSession;
import xapi.util.X_Debug;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public class GwtcRemoteProcessJob extends GwtcJob {

    private final ShellSession process;
    private final GwtcJobMonitorImpl monitor;

    public GwtcRemoteProcessJob(GwtManifest manifest, ShellSession process) {
        super(manifest);
        this.process = process;
        LinkedBlockingDeque<String> fromCompiler = new LinkedBlockingDeque<>();
        LineReader stdOut = new SimpleLineReader() {
            @Override
            public void onLine(String line) {
                try {
                    fromCompiler.put(line);
                } catch (InterruptedException e) {
                    throw X_Debug.rethrow(e);
                }
            }
        };
        LineReader stdErr = new SimpleLineReader() {
            @Override
            public void onLine(String line) {
                // error messages should go straight to log...
                // TODO: grab log level from beginning of line (if present).
                X_Log.trace(GwtcRemoteProcessJob.class, line);
            }
        };
        process.stdOut(stdOut);
        process.stdErr(stdErr);
        this.monitor = new GwtcJobMonitorImpl(
            fromCompiler::take,
            process::stdIn);

        manifest.setOnline(true);
        X_Process.runDeferred(()->{
            while (manifest.isOnline()) {
                try {
                    flushMonitor();
                } catch (Throwable t) {
                    X_Log.warn(GwtcJob.class, "Failed to flush remote process monitor; thread bailing", t);
                    manifest.setOnline(false);
                    process.destroy();
                    throw t;
                }
            }
        });
    }

    @Override
    protected GwtcJobMonitor getMonitor() {
        return monitor;
    }
}

package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileStatus;
import xapi.dev.gwtc.api.GwtcService;
import xapi.fu.X_Fu;
import xapi.gwtc.api.GwtManifest;
import xapi.io.api.LineReader;
import xapi.io.api.LineReaderWithLogLevel;
import xapi.io.api.SimpleLineReader;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.process.X_Process;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellSession;
import xapi.util.X_Debug;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public class GwtcRemoteProcessJob extends GwtcJob {

    private final ShellSession process;
    private final GwtcJobMonitorImpl monitor;

    public GwtcRemoteProcessJob(GwtManifest manifest, GwtcService service) {
        super(manifest);

        Integer debugPort = manifest.getDebugPort();
        if (debugPort != null) {
            manifest.addJvmArg("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort);
        }
        final String[] jvmArgs = manifest.toJvmArgArray();
        String[] programArgs = manifest.toProgramArgArray();
        if (manifest.getLogFile() != null) {
            programArgs = X_Fu.concat(new String[]{"-logFile", manifest.getLogFile()}, programArgs);
        }
        programArgs = X_Fu.concat(
            manifest.isRecompile() ? GwtcJobMonitor.JOB_RECOMPILE : GwtcJobMonitor.JOB_COMPILE,
            programArgs
        );

        final String[] cp = manifest.toClasspathFullCompile(service);

        LinkedBlockingDeque<String> fromCompiler = new LinkedBlockingDeque<>();
        LineReader stdOut = new LineReaderWithLogLevel() {
            @Override
            public void onLine(String line) {
                if (!line.trim().isEmpty()) {
                    try {
                        fromCompiler.put(line);
                    } catch (InterruptedException e) {
                        throw X_Debug.rethrow(e);
                    }
                }
            }
        }.setLogLevel(LogLevel.DEBUG);
        LineReader stdErr = new LineReaderWithLogLevel() {
            @Override
            public void onLine(String line) {
                // error messages should go straight to log...
                // TODO: grab log level from beginning of line (if present).
                line = line.trim();
                if (!line.isEmpty()) {
                    logLine(line);
                }
            }
        }.setLogLevel(LogLevel.DEBUG);

        final ShellSession process = X_Shell.launchJava(
            GwtcJobManagerImpl.class,
            cp,
            jvmArgs,
            programArgs,
            stdOut,
            stdErr
        );

        this.process = process;

        this.monitor = new GwtcJobMonitorImpl(
            fromCompiler::take,
            process::stdIn,
            ()->!fromCompiler.isEmpty()
        );

        manifest.setOnline(true);
        scheduleFlusher(manifest, process::destroy);
    }

    private void logLine(String line) {
        X_Log.info(GwtcRemoteProcessJob.class, line);
    }

    @Override
    protected GwtcJobMonitor getMonitor() {
        return monitor;
    }
}

package xapi.dev.gwtc.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcService;
import xapi.fu.X_Fu;
import xapi.fu.itr.ArrayIterable;
import xapi.gwtc.api.GwtManifest;
import xapi.io.api.LineReader;
import xapi.io.api.LineReaderWithLogLevel;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.model.api.PrimitiveSerializer;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellSession;
import xapi.util.X_Debug;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public class GwtcRemoteProcessJob extends GwtcJob {

    private final ShellSession process;
    private final GwtcJobMonitorImpl monitor;

    public GwtcRemoteProcessJob(
        GwtManifest manifest,
        GwtcService service,
        PrimitiveSerializer serializer
    ) {
        super(manifest, serializer);

        Integer debugPort = manifest.getDebugPort();
        if (debugPort != null) {
            manifest.addJvmArg("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort);
        }
        String[] jvmArgs = manifest.toJvmArgArray();
        String[] programArgs = manifest.toProgramArgArray();
        if (manifest.getLogFile() != null) {
            programArgs = X_Fu.concat(new String[]{"-logFile", manifest.getLogFile()}, programArgs);
        }

        // We want to respect tmp dir, since some people may want to check in prebuilt files.
        String tmpDir = System.getProperty("java.io.tmpdir");
        tmpSearch:
        if (tmpDir != null) {
            for (String jvmArg : jvmArgs) {
                if (jvmArg.startsWith("-Djava.io.tmpdir")) {
                    break tmpSearch;
                }
            }
            jvmArgs = X_Fu.concat(jvmArgs, "-Djava.io.tmpdir="+tmpDir);
        }

        programArgs = X_Fu.concat(
            manifest.isRecompile() ? GwtcJobMonitor.JOB_RECOMPILE : GwtcJobMonitor.JOB_COMPILE,
            programArgs
        );

        String[] cp = manifest.toClasspathFullCompile();
        final URL[] asUrls = ArrayIterable.iterate(cp)
            .map(item->"file:" + item)
            .mapUnsafe(URL::new).toArray(URL[]::new);
        final URLClassLoader urlLoader = new URLClassLoader(asUrls, null);
        final URLClassLoader fixedUp = service.ensureMeetsMinimumRequirements(urlLoader);
        if (fixedUp != urlLoader) {
            // service had to add some items.  Lets pull them off and concat to our existing cp.
            IntTo<String> all = X_Collect.newList(String.class, X_Collect.MUTABLE_INSERTION_ORDERED_SET);
            all.addAll(cp);
            X_Log.warn(GwtcRemoteProcessJob.class, "Had to fixup classpath urls", fixedUp.getURLs());
            all.addAll(
                ArrayIterable.iterate(fixedUp.getURLs())
                    .map(URL::toExternalForm)
                    .map(s->s.replace("file:", ""))
            );
            cp = all.toArray(String[]::new);
        }

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

        process = X_Shell.launchJava(
            GwtcJobManagerImpl.class,
            cp,
            jvmArgs,
            programArgs,
            stdOut,
            stdErr
        );

        this.monitor = new GwtcJobMonitorImpl(
            fromCompiler::take,
            process::stdIn,
            ()->!fromCompiler.isEmpty()
        );

        manifest.setOnline(true);
        scheduleFlusher(manifest, process::destroy);
    }

    @Override
    protected void finalize() throws Throwable {
        if (process != null) {
            process.destroy();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (process != null) {
            process.destroy();
        }
    }

    private void logLine(String line) {
        X_Log.info(GwtcRemoteProcessJob.class, line);
    }

    @Override
    public GwtcJobMonitor getMonitor() {
        return monitor;
    }
}

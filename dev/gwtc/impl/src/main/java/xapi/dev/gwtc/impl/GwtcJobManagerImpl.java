package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobManager;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileStatus;
import xapi.dev.gwtc.api.GwtcService;
import xapi.except.NotYetImplemented;
import xapi.fu.X_Fu;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.PrimitiveSerializerDefault;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellSession;
import xapi.source.api.CharIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.util.SortedSet;

import static xapi.dev.gwtc.api.GwtcJobMonitor.ARG_LOG_FILE;

import com.google.gwt.dev.CompileTaskRunner;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.Compiler.ArgProcessor;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.CompilerOptionsImpl;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.codeserver.CompileStrategy;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public class GwtcJobManagerImpl extends GwtcJobManager {

    private final PrimitiveSerializer serializer;

    public GwtcJobManagerImpl(GwtcService service) {
        super(service);
        serializer = newSerializer();
    }

    protected PrimitiveSerializer newSerializer() {
        return new PrimitiveSerializerDefault();
    }


    @Override
    protected GwtcJob launchJob(GwtManifest manifest) {
        final String gwtHome = service.generateCompile(manifest);
        if (manifest.isJ2cl()) {
            // TODO: hook this up w/ vertispan repo added to MvnService
            throw new NotYetImplemented("J2cl not yet public :-(");
        }
        final GwtcJob job;
        if (manifest.isUseCurrentJvm()) {
            final URLClassLoader classpath = service.resolveClasspath(manifest, gwtHome);
            // When using the current jvm, we want to launch a thread with our classloader
            // We will talk to the other thread a shared monitor that has reflection proxies built in.
            job = new GwtcLocalProcessJob(manifest, classpath, gwtHome);


        } else {
            // When using a new process, we want must launch the process and bind it to a monitor
            final String[] jvmArgs = manifest.toJvmArgArray();
            final String[] programArgs = X_Fu.concat(manifest.toProgramArgArray(),
                manifest.isRecompile() ? GwtcJobMonitor.JOB_RECOMPILE : GwtcJobMonitor.JOB_COMPILE
            );
            // TODO: ensure classpath has our gwtc impl embedded...
            final String[] cp = manifest.toClasspathFullCompile(gwtHome);
            final ShellSession process = X_Shell.launchJava(
                GwtcJobMonitorImpl.class,
                cp,
                jvmArgs,
                programArgs
            );
            job = new GwtcRemoteProcessJob(manifest, process);

        }

        return job;
    }


    public static void main(String ... args)
    throws NoSuchFieldException, IllegalAccessException, InterruptedException, IOException {
        if (args.length == 0) {
            args = new String[]{"help"};
        }
        // We were called as a main, so we should expect to use System.in/out for communication,
        // and only be able to access the *AsCompiler methods (the *AsCaller should throw errors if used)
        final GwtcJobMonitorImpl monitor = new GwtcJobMonitorImpl(System.in, System.out);
        GwtcService compiler = X_Inject.instance(GwtcService.class);
        new GwtcJobManagerImpl(compiler)
            .parseArgs(monitor, args);
    }

    public void parseArgs(GwtcJobMonitorImpl monitor, String[] args) throws IOException {

        monitor.updateCompileStatus(CompileStatus.Preparing);

        String command = args[0];
        args = X_Fu.slice(1, args.length, args);

        // Now, check for log file
        String logFile = null;
        if (ARG_LOG_FILE.equals(command)) {
            logFile = args[1];
            command = args[2];
            args = X_Fu.slice(3, args.length, args);
        } else if (args.length > 0 && ARG_LOG_FILE.equals(args[0])) {
            logFile = args[1];
            args = X_Fu.slice(2, args.length, args);
        }

        if (logFile != null) {
            if (GwtcJobMonitor.STD_OUT_TO_STD_ERR.equals(logFile)) {
                // we are going to use stdErr for all program output
                System.setErr(System.out);
                // in this case, we are not going to further indirect output streams
                // (no files are piped to in this case; you just get everything interleaved on System.err)
                logFile = null;
            } else if (GwtcJobMonitor.NO_LOG_FILE.equals(logFile)) {
                // user sent `-logFile nlf` to tell us to not touch System streams.
                // This is likely somebody calling our main method from other running java code.
                logFile = null;
            }
        } else {
            // If no logFile is specified, we want to use a file in the tmp dir
            logFile = File.createTempFile("gwtcJob", "log").getAbsolutePath();
        }

        if (logFile != null) {
            File f = new File(logFile);
            if (!f.exists()) {
                if (!f.getParentFile().exists()) {
                    X_Log.error(GwtcJobMonitorImpl.class, "Specified -logFile", logFile, " does not exist, nor does it's host directory");
                    throw new IllegalStateException("Bad logFile; ensure parent directory exists: " + logFile);
                }
                boolean success = f.createNewFile();
                if (!success) {
                    X_Log.warn(GwtcJobMonitorImpl.class, "Unable to create logFile", logFile, "expect more errors...");
                }
            }
            PrintStream fout = new PrintStream(new FileOutputStream(f));
            System.setOut(fout);
            System.setErr(fout);
        }

        switch (command) {
            case "recompile":
                runRecompile(monitor, args);
                break;
            case "compile":
                runCompile(monitor, logFile, args);
                break;
            case "test":
                runTestMode(monitor, args);
                break;
            case "help":
                printHelp(args);
                monitor.updateCompileStatus(CompileStatus.Success);
                break;
            default:
                X_Log.info(GwtcJobMonitorImpl.class, "Incorrect arguments; must begin with one of: " +
                    "recompile|compile|test|help.  You sent: ", args);
                monitor.updateCompileStatus(CompileStatus.Failed);
        }
    }

    private void printHelp(String[] args) {
    }

    protected void runTestMode(GwtcJobMonitorImpl monitor, String[] args) {
        for (String arg : args) {
            monitor.writeAsCompiler(arg);
        }
        monitor.updateCompileStatus(CompileStatus.Success);

    }

    protected void runRecompile(GwtcJobMonitorImpl monitor, String[] args) {
    }


    protected void runCompile(GwtcJobMonitorImpl monitor, String logFile, String[] args) {
        // All standard arguments to the compiler should have already been supplied as arguments to our main.
        final CompilerOptions options = new CompilerOptionsImpl();
        final ArgProcessor processor = new ArgProcessor(options);
        if (!processor.processArgs(args)) {
            // Send reason why?
            monitor.updateCompileStatus(CompileStatus.Failed);
        }
        CompileTask task = logger -> {
            boolean success = new Compiler().compile(logger, options);
            return success;
        };
        boolean result = CompileTaskRunner.runWithAppropriateLogger(options, task);

        if (result) {
            // Now, we send back the assembled CompileDir...
            //            protected String uri;
            //            protected String warDir;
            //            protected String workDir;
            //            protected String deployDir;
            //            protected String extraDir;
            //            protected String genDir;
            //            protected String logFile;
            //            protected String sourceDir;
            //            protected Map<String, String> userAgentMap;
            //            protected int port;
            //            private CompileStrategy strategy;
            sendCompilerResults(monitor,
                options.getWarDir().getAbsolutePath(),
                options.getWorkDir().getAbsolutePath(),
                options.getDeployDir().getAbsolutePath(),
                options.getExtraDir().getAbsolutePath(),
                options.getGenDir().getAbsolutePath(),
                logFile,
                new File(options.getExtraDir(), options.getModuleNames().get(0) + "/symbolMaps").getAbsolutePath(),
                //                options.getSaveSourceOutput().getAbsolutePath(),
                options.getFinalProperties().getBindingProperties(),
                options.isIncrementalCompileEnabled() ? CompileStrategy.INCREMENTAL : CompileStrategy.FULL
            );
        } else {
            monitor.updateCompileStatus(CompileStatus.Failed);
        }
    }

    private void sendCompilerResults(
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

        monitor.updateCompileStatus(CompileStatus.Success,
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


}

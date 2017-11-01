package xapi.dev.gwtc.impl;

import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobManager;
import xapi.dev.gwtc.api.GwtcJobMonitor;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileStatus;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.gwtc.api.IsAppSpace;
import xapi.except.NotYetImplemented;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.X_Fu;
import xapi.fu.iterate.ArrayIterable;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.inject.X_Inject;
import xapi.io.api.LineReaderWithLogLevel;
import xapi.log.X_Log;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.PrimitiveSerializerDefault;
import xapi.process.X_Process;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellSession;
import xapi.source.api.CharIterator;
import xapi.time.X_Time;
import xapi.util.X_Debug;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompileTaskRunner;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.Compiler.ArgProcessor;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.CompilerOptionsImpl;
import com.google.gwt.dev.MinimalRebuildCacheManager;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.codeserver.*;
import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.dev.codeserver.JobEvent.Status;
import com.google.gwt.dev.javac.StaleJarError;
import com.google.gwt.dev.javac.UnitCache;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public class GwtcJobManagerImpl extends GwtcJobManager {

    private final PrimitiveSerializer serializer;
    private String currentEventId;
    private Status status;
    private String logFile;

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
            job = new GwtcRemoteProcessJob(manifest, service);

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

    public void parseArgs(GwtcJobMonitor monitor, String[] args) throws IOException {

        monitor.updateCompileStatus(CompileStatus.Preparing);


        GwtcArgProcessor managerArgs = new GwtcArgProcessor();
        args = processArgs(managerArgs, args);
        String command = args[0];
        args = X_Fu.slice(1, args.length, args);

        switch (command) {
            case "recompile":
                runRecompile(monitor, managerArgs, args);
                break;
            case "compile":
                runCompile(monitor, logFile, managerArgs, args);
                break;
            case "test":
                runTestMode(monitor, args);
                break;
            case "help":
                printHelp(args);
                monitor.updateCompileStatus(CompileStatus.Success);
                break;
            default:
                X_Log.error(GwtcJobMonitorImpl.class, "Incorrect arguments; must begin with one of: " +
                    "recompile|compile|test|help.  You sent: ", command, "\nRemaining args:", args);
                monitor.updateCompileStatus(CompileStatus.Failed);
        }
    }

    private String[] processArgs(GwtcArgProcessor managerArgs, String[] args) throws IOException {
        args = managerArgs.processArgs(args);

        // Now, grab our log file
        final File log = managerArgs.getLogFile();

        if (log != null) {
            if (GwtcJobMonitor.STD_OUT_TO_STD_ERR.equals(log.getName())) {
                // we are going to use stdErr for all program output
                System.setOut(System.err);
                // in this case, we are not going to further indirect output streams
                // (no files are piped to in this case; you just get everything interleaved on System.err)
                logFile = null;
            } else if (GwtcJobMonitor.NO_LOG_FILE.equals(log.getName())) {
                // user sent `-logFile nlf` to tell us to not touch System streams.
                // This is likely somebody calling our main method from other running java code.
                logFile = null;
            } else {
                logFile = log.getAbsolutePath();
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
        return args;
    }

    private void printHelp(String[] args) {
    }

    protected void runTestMode(GwtcJobMonitor monitor, String[] args) {
        for (String arg : args) {
            monitor.writeAsCompiler(arg);
        }
        monitor.updateCompileStatus(CompileStatus.Success);

    }

    protected void runRecompile(GwtcJobMonitor monitor, GwtcArgProcessor managerArgs, String[] args) {
        GwtcEventTable table = new GwtcEventTable();
        final String moduleName = args[args.length-1]; // by convention, module name is always last
        final IsAppSpace app = SuperDevUtil.newAppSpace(moduleName);
        final OutboxDir outboxDir;
        final LauncherDir launcher;
        final Options opts;

//        RecompileRunner runner = new RecompileRunner();
        final String name = "myname";
        table.listenForEvents(name, Status.COMPILING, false, ev->{
            currentEventId = ev.getJobId();
            status = ev.getStatus();
        });
        table.listenForEvents(name, null, false, ev->{
            X_Log.info(GwtcJobStateImpl.class, "Gwt Event", ev.getJobId(), "status:", ev.getStatus());
            if (ev.getJobId().equals(currentEventId)){
                X_Log.info(GwtcJobStateImpl.class, "Setting job", ev.getJobId(), "status to ", ev.getStatus());
                status = ev.getStatus();
            }
        });

        opts = new Options();
        final File cacheDir = managerArgs.getUnitCacheDir();

        opts.parseArgs(args);
        // We always want to disable Outbox precompile so that we control the job created.
        opts.setNoPrecompile(true);
        final TreeLogger logger = createLogger(opts);

        try {
            outboxDir = OutboxDir.create(opts.getLauncherDir(), logger);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        launcher = LauncherDir.maybeCreate(opts);

        final JJSOptionsImpl options = new JJSOptionsImpl();
        // The following two options are the only ones used by UnitCacheSingleton;
        // we should put back / in support for these...
        //    options.setGenerateJsInteropExports(false);
        //    options.getJsInteropExportFilter().add("...");
        final UnitCache cache = UnitCacheSingleton.get(logger, cacheDir, options);
        final MinimalRebuildCacheManager rebinds = new MinimalRebuildCacheManager(logger, cacheDir, new HashMap<>());

        Recompiler recompiler = new Recompiler(outboxDir, launcher, moduleName.split("/")[0], opts, cache, rebinds);

        final RecompileRunner runner = new RecompileRunner(table, rebinds, moduleName, app);
        final ResourceLoader currentLoader = recompiler.getResourceLoader();

        table.listenForEvents(name, Status.SERVING, false, ev->{
            if (ev.getJobId().equals(currentEventId)) {
                X_Time.runLater(()->
                    sendCompilerResults(monitor, ev.getCompileDir(), opts, ev.getCompileStrategy())
                );
            }
        });


        Map<String, String> defaultProps = getDefaultProps(opts, args);
        Result dir;
        final Outbox box;
        try {
            box = new Outbox(moduleName, recompiler, opts, logger);
        } catch (UnableToCompleteException e) {
            logger.log(TreeLogger.ERROR, "Unable to prepare recompiler", e);
            monitor.updateCompileStatus(CompileStatus.Failed, "Unable to prepare recompiler");
            throw new RuntimeException(e);
        }
        try{
            final Job job = new Job(box, defaultProps, logger, opts);
            runner.submit(job);
            dir = job.waitForResult();
            final CompileStrategy strategy = runner.getTable().getPublishedEvent(job).getCompileStrategy();
            CompileDir result = dir.getOutputDir();
            sendCompilerResults(monitor, result, opts, strategy);
//            return Out3.out3(dir, strategy, currentLoader);
        }catch (Throwable e) {
            monitor.updateCompileStatus(CompileStatus.Failed, "Compilation failed (check logs): " + e);
            if (e instanceof StaleJarError) {
                // TODO: forcibly discard this thread and start a new one with a fresh classpath
                logger.log(TreeLogger.WARN, "Stale jars detected; reinitializing GWT compiler");
            }
            e.printStackTrace();
            logger.log(TreeLogger.ERROR, "Unable to compile module.", e);
            throw new RuntimeException(e);
        }
        // Now that we have successfully compiled, lets stay alive to listen for requests to service
        keepAlive(monitor, ()->{
            try {
                final Job newJob = new Job(box, defaultProps, logger, opts);
                final Result newDir = newJob.waitForResult();
                final CompileStrategy strategy = runner.getTable().getPublishedEvent(newJob).getCompileStrategy();
                CompileDir result = newDir.getOutputDir();
                sendCompilerResults(monitor, result, opts, strategy);
            } catch (Throwable t) {
                monitor.updateCompileStatus(CompileStatus.Failed, "Recompile failed: " + t.toString());
                X_Log.error(GwtcJobManagerImpl.class, "Recompile failed", t);
            }

        });
    }

    protected Map<String,String> getDefaultProps(Options opts, String[] args) {
        final Map<String, String> defaultProps = new HashMap<String, String>();
        defaultProps.put("user.agent", "safari");
        defaultProps.put("locale", "en");
        defaultProps.put("compiler.useSourceMaps", "true");
        return defaultProps;
    }

    protected TreeLogger createLogger(Options opts) {
        final PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
        logger.setMaxDetail(opts.getLogLevel());
        return logger;
    }

    protected void runCompile(GwtcJobMonitor monitor, String logFile, GwtcArgProcessor managerArgs, String[] args) {
        // All standard arguments to the compiler should have already been supplied as arguments to our main.
        final CompilerOptions options = new CompilerOptionsImpl();
        final ArgProcessor processor = new ArgProcessor(options);
        if (!processor.processArgs(args)) {
            // Send reason why?
            monitor.updateCompileStatus(CompileStatus.Failed, "Failed to process args: " +
                ArrayIterable.iterate(args).join("|", "|", "|")
            );
        }
        CompileTask task = logger -> {
            boolean success = new Compiler().compile(logger, options);
            return success;
        };
        DoUnsafe runCompile = () -> {

            boolean result = CompileTaskRunner.runWithAppropriateLogger(options, task);

            if (result) {
                // Now, we send back the assembled CompileDir...
                sendCompilerResults(monitor,
                    path(options.getWarDir()),
                    path(options.getWorkDir()),
                    path(options.getDeployDir()),
                    path(options.getExtraDir()),
                    path(options.getGenDir()),
                    logFile,
                    new File(options.getExtraDir(), options.getModuleNames().get(0) + "/symbolMaps").getAbsolutePath(),
                    //                options.getSaveSourceOutput().getAbsolutePath(),
                    (options.getFinalProperties() == null ? null : options.getFinalProperties().getBindingProperties()),
                    options.isIncrementalCompileEnabled() ? CompileStrategy.INCREMENTAL : CompileStrategy.FULL
                );
            } else {
                monitor.updateCompileStatus(CompileStatus.Failed);
            }
        };
        runCompile.done();

        // Now, setup a thread to keep us alive, listening for messages to either recompile or die
        keepAlive(monitor, runCompile);
    }

    private void keepAlive(GwtcJobMonitor monitor, DoUnsafe runCompile) {
        X_Process.runDeferred(()->{
            while (true) {
                String message = monitor.readAsCompiler();
                switch (message) {
                    case GwtcJobMonitor.JOB_COMPILE:
                    case GwtcJobMonitor.JOB_RECOMPILE:
                        runCompile.done();
                        break;
                    case GwtcJobMonitor.JOB_DIE:
                        return;
                    default:
                        X_Log.error(GwtcJobMonitorImpl.class, "Unsupported gwt process message ", message);
                }
            }
        });
    }

    private void sendCompilerResults(
        GwtcJobMonitor monitor,
        CompileDir dir,
        Options options,
        CompileStrategy strategy
    ) {
        sendCompilerResults(monitor,
            path(dir.getWarDir()),
            path(dir.getWorkDir()),
            path(dir.getDeployDir()),
            path(dir.getExtraDir()),
            path(dir.getGenDir()),
            path(dir.getLogFile()),
            new File(options.getLauncherDir().getAbsolutePath()/*getExtraDir()*/, options.getModuleNames().get(0) + "/symbolMaps").getAbsolutePath(),
            null,
            strategy
        );
    }

    private String path(File file) {
        return file == null ? null : file.getAbsolutePath();
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

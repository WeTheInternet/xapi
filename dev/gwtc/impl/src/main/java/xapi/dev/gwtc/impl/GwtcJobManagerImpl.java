package xapi.dev.gwtc.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.gwtc.api.*;
import xapi.dev.gwtc.api.GwtcJobMonitor.CompileMessage;
import xapi.except.NotYetImplemented;
import xapi.fu.*;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.Out1.Out1Unsafe;
import xapi.fu.itr.ArrayIterable;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Util;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static xapi.fu.itr.ArrayIterable.iterate;
import static xapi.time.X_Time.diff;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.*;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.CompilerOptionsImpl;
import com.google.gwt.dev.Compiler.ArgProcessor;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.codeserver.*;
import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.dev.codeserver.JobEvent.Status;
import com.google.gwt.dev.javac.UnitCache;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.resource.impl.ResourceAccumulatorManager;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/14/17.
 */
public class GwtcJobManagerImpl extends GwtcJobManagerAbstract {

    private static final long Process_TTL_Millis = TimeUnit.MINUTES.toMillis(45);
    private final StringTo<String> eventIds;
    private final StringTo<String> logFiles;

    public GwtcJobManagerImpl(GwtcService service) {
        this(service, X_Collect.newStringMap(String.class));
    }

    public GwtcJobManagerImpl(GwtcService service, StringTo<String> logFiles) {
        super(service);
        eventIds = X_Collect.newStringMap(String.class);
        this.logFiles = logFiles;
    }

    @Override
    protected GwtcJob launchJob(GwtManifest manifest) {
        if (!service.hasProject(manifest.getModuleName())) {
            service.generateCompile(manifest);
        }
        if (manifest.isJ2cl()) {
            // TODO: hook this up w/ vertispan repo added to MvnService
            throw new NotYetImplemented("J2cl not yet public :-(");
        }
        final GwtcJob job;
        if (manifest.isUseCurrentJvm()) {
            final URLClassLoader classpath = service.resolveClasspath(manifest);
            // When using the current jvm, we want to launch a thread with our classloader
            // We will talk to the other thread a shared monitor that has reflection proxies built in.
            job = new GwtcLocalProcessJob(manifest, classpath, serializer);
        } else {
            // When using a new process, we want must launch the process and bind it to a monitor
            job = new GwtcRemoteProcessJob(manifest, service, serializer);

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
        GwtcArgProcessor managerArgs = new GwtcArgProcessor();
        final StringTo<String> logFiles = X_Collect.newStringMap(String.class);
        try {
            args = processArgs(managerArgs, args, logFiles);
        } catch (IOException e) {
            X_Log.error(GwtcJobManagerImpl.class, "Error parsing arguments", e);
            monitor.updateCompileStatus(CompileMessage.Failed);
            return;
        }
        GwtcService compiler = X_Inject.instance(GwtcService.class);
        final GwtcJobManagerImpl manager = new GwtcJobManagerImpl(compiler, logFiles);
        manager.runJob(monitor, managerArgs, args);
    }

    public void runJob(GwtcJobMonitor monitor, GwtcArgProcessor managerArgs, String[] args) {

        monitor.updateCompileStatus(CompileMessage.Preparing);

        try {

            String command = args[0];
            args = X_Fu.slice(1, args.length, args);

            switch (command) {
                case "recompile":
                    runRecompile(monitor, managerArgs, args);
                    break;
                case "compile":
                    runCompile(monitor, managerArgs, args);
                    break;
                case "test":
                    runTestMode(monitor, args);
                    break;
                case "help":
                    printHelp(args);
                    monitor.updateCompileStatus(CompileMessage.Success);
                    break;
                default:
                    X_Log.error(GwtcJobMonitorImpl.class, "Incorrect arguments; must begin with one of: " +
                        "recompile|compile|test|help.  You sent: ", command, "\nRemaining args:", args);
                    monitor.updateCompileStatus(CompileMessage.Failed);
            }
        } catch (Throwable t) {
            monitor.updateCompileStatus(CompileMessage.Failed, t.toString(),
                ArrayIterable.iterate(t.getStackTrace()).join("\n"));
            throw t;
        }

    }

    private static String[] processArgs(
        GwtcArgProcessor managerArgs,
        String[] args,
        StringTo<String> logFiles
    ) throws IOException {
        args = managerArgs.processArgs(args);

        // Now, grab our log file
        final File log = managerArgs.getLogFile();
        String logFile;
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
        // the entry point module is always the last argument
        logFiles.put(args[args.length-1], logFile);
        return args;
    }

    protected void printHelp(String[] args) {
    }

    protected void runTestMode(GwtcJobMonitor monitor, String[] args) {
        for (String arg : args) {
            monitor.writeAsCompiler(CompileMessage.KEY_LOG + arg);
        }
        monitor.updateCompileStatus(CompileMessage.Success);

    }

    protected void runRecompile(GwtcJobMonitor monitor, GwtcArgProcessor managerArgs, String[] args) {
        GwtcEventTable table = new GwtcEventTable();
        final String moduleName = args[args.length-1]; // by convention, module name is always last
        final IsAppSpace app = SuperDevUtil.newAppSpace(moduleName);
        final OutboxDir outboxDir;
        LauncherDir launcher;
        final Options opts;
        final String name = args[args.length - 1];
        table.listenForEvents(name, Status.COMPILING, false, ev->{
            String mod = ev.getInputModuleName();
            eventIds.put(mod, ev.getJobId());
            final CompileMessage status = convertStatus(ev.getStatus());
            statuses.put(mod, status);
        });
        table.listenForEvents(name, null, false, ev->{
            X_Log.trace(GwtcJobManagerImpl.class, "Gwt Event", ev.getJobId(), "status:", ev.getStatus());
            final String mod = ev.getInputModuleName();
            final String currentId = eventIds.get(mod);
            if (ev.getJobId().equals(currentId)){
                final CompileMessage state = convertStatus(ev.getStatus());
                final CompileMessage was = statuses.put(mod, state);
                if (was == null) {
                    X_Log.info(GwtcJobManagerImpl.class, "Setting job", ev.getJobId(), "status to", ev.getStatus());
                } else if (was != state) {
                    X_Log.info(GwtcJobManagerImpl.class, "Changing job", ev.getJobId(), "status from", was, "to", ev.getStatus());
                }
                runningJobs.getMaybe(mod)
                    .readIfPresent2(GwtcJob::setState, state);
            }
        });

        opts = new Options();
        final File cacheDir = managerArgs.getUnitCacheDir();
        final ArrayIterable<String> argList = iterate(args);
        if (argList.noneMatch(String::startsWith, "-launcherDir")) {
            if (argList.noneMatch(String::startsWith, "-war")) {
                monitor.updateCompileStatus(CompileMessage.Failed,
                    "Must specify -launcherDir or -war. You sent: " +argList.join(" "));
                throw new IllegalArgumentException("Must specify -launcherDir or -war");
            }
        }
        opts.parseArgs(args);
        // We always want to disable Outbox precompile so that we control the job created.
        opts.setNoPrecompile(true);
        final TreeLogger logger = createLogger(opts);

        launcher = LauncherDir.maybeCreate(opts);
        try {
            outboxDir = OutboxDir.create(opts.getLauncherDir(), logger);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final JJSOptionsImpl options = new JJSOptionsImpl();
        // The following two options are the only ones used by UnitCacheSingleton;
        // we should put back / in support for these...
        //    options.setGenerateJsInteropExports(false);
        //    options.getJsInteropExportFilter().add("...");
        final UnitCache cache = UnitCacheSingleton.get(logger, cacheDir, options);
        final MinimalRebuildCacheManager rebinds = new MinimalRebuildCacheManager(logger, cacheDir, new HashMap<>());

        Recompiler recompiler = new Recompiler(outboxDir, launcher, moduleName.split("/")[0], opts, cache, rebinds);

        final RecompileRunner runner = new RecompileRunner(table, rebinds, moduleName, app);

        table.listenForEvents(name, Status.SERVING, false, ev->{
            final String currentId = eventIds.get(ev.getInputModuleName());
            if (ev.getJobId().equals(currentId)) {
                X_Time.runLater(()->
                    sendCompilerResults(monitor, ev.getOutputModuleName(), ev.getCompileDir(), opts, ev.getCompileStrategy())
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
            monitor.updateCompileStatus(CompileMessage.Failed, "Unable to prepare recompiler");
            throw new RuntimeException(e);
        }
        final Job job;
        try{
            job = new Job(box, defaultProps, logger, opts);
            runner.submit(job);
            dir = job.waitForResult();
            final CompileStrategy strategy = runner.getTable().getPublishedEvent(job).getCompileStrategy();
            CompileDir result = dir.getOutputDir();
            if (result == null) {
                if (strategy == CompileStrategy.SKIPPED) {
                    monitor.updateCompileStatus(CompileMessage.Success);
                } else {
                    // terminal error.  Just die.
                    logger.log(TreeLogger.ERROR, "Terminal error encountered; gwtc shutting down");
                    die(monitor);
                    return;
                }
            } else {
                sendCompilerResults(monitor, dir.getOutputModuleName(), result, opts, strategy);
            }
        }catch (Throwable e) {
            monitor.updateCompileStatus(CompileMessage.Failed, "Compilation failed (check logs): " + e);
            if (e.getClass().getName().contains("StaleJar")) {
                // TODO: forcibly discard this thread and start a new one with a fresh classpath
                logger.log(TreeLogger.WARN, "Stale jars detected; reinitializing GWT compiler");
            }
            e.printStackTrace();
            logger.log(TreeLogger.ERROR, "Unable to compile module.", e);
            throw new RuntimeException(e);
        }
        // Now that we have successfully compiled, lets stay alive to listen for requests to service
        final ResourceLoader currentLoader = recompiler.getResourceLoader();
        keepAlive(monitor,
            currentLoader::getResource,
            ()->{
            // freshness check task
                Mutable<Boolean> result = new Mutable<>();
                recompiler.checkCompileFreshness(()->{
                    result.in(true);
                    synchronized (result) {
                        result.notify();
                    }
                }, ()->{
                    result.in(false);
                    synchronized (result) {
                        result.notify();
                    }
                });
                synchronized (result) {
                    try {
                        if (result.isNull()) {
                            result.wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return result.out1();
            }, ()->{
            // recompile task
            try {

                final Job newJob = new Job(box, defaultProps, logger, opts);
                runner.submit(newJob);
                final Result newDir = newJob.waitForResult();
                final CompileStrategy strategy = runner.getTable().getPublishedEvent(newJob).getCompileStrategy();
                CompileDir result = newDir.getOutputDir();
                if (newDir.getError() != null) {
                    monitor.updateCompileStatus(CompileMessage.Failed, newDir.getError().toString());
                    X_Log.error(GwtcJobManagerImpl.class, "Recompile failed for", newDir.getOutputModuleName(), newDir.getError());
                } else {
                    sendCompilerResults(monitor, newDir.getOutputModuleName(), result, opts, strategy);
                }
            } catch (Throwable t) {
                monitor.updateCompileStatus(CompileMessage.Failed, "Recompile failed: " + t.toString());
                X_Log.error(GwtcJobManagerImpl.class, "Recompile failed", t);
                die(monitor);
            }

        });
    }

    private CompileMessage convertStatus(Status status) {
        switch (status) {
            case GONE:
                return CompileMessage.Destroyed;
            case COMPILING:
                return CompileMessage.Running;
            case ERROR:
                return CompileMessage.Failed;
            case SERVING:
                return CompileMessage.Success;
            case WAITING:
                return CompileMessage.Preparing;
            default:
                throw new IllegalArgumentException("Cannot convert " + status);
        }
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

    protected void runCompile(GwtcJobMonitor monitor, GwtcArgProcessor managerArgs, String[] args) {
        final String module = args[args.length - 1];
        setState(module, monitor, CompileMessage.Preparing, module);
        String logFile = managerArgs.getLogFile() == null ? null : managerArgs.getLogFile().getAbsolutePath();
        // All standard arguments to the compiler should have already been supplied as arguments to our main.
        final CompilerOptions options = new CompilerOptionsImpl();
        final ArgProcessor processor = new ArgProcessor(options);
        if (!processor.processArgs(args)) {
            // Send reason why?
            monitor.updateCompileStatus(CompileMessage.Failed, "Failed to process args: " +
                iterate(args).join("|", "|", "|")
            );
        }

        CompileTask task = logger -> {
            boolean success = Compiler.compile(logger, options);
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
                monitor.updateCompileStatus(CompileMessage.Failed);
            }
        };
        setState(module, monitor, CompileMessage.Running, module);
        runCompile.done();

        // Now, setup a thread to keep us alive, listening for messages to either recompile or die
        final Out1Unsafe<Boolean> checkFreshness = ()->{

            Mutable<Boolean> result = new Mutable<>();
            ResourceAccumulatorManager.checkCompileFreshness(
                ()->{
                    result.in(true);
                    synchronized (result) {
                        result.notify();
                    }
                },
                ()->{
                    result.in(false);
                    synchronized (result) {
                        result.notify();
                    }
                },
                false
            );
            synchronized (result) {
                result.wait();
            }
            return result.out1();
        };
        // gross.  We should be able to access a single shared loader that stays primed :-/
        final ResourceLoader loader = ResourceLoaders.fromContextClassLoader();
        keepAlive(monitor, loader::getResource, checkFreshness, runCompile);
    }

    protected void setState(String module, GwtcJobMonitor monitor, CompileMessage state, String extra) {
        monitor.writeAsCompiler(state.controlChar() + extra);
        statuses.put(module, state);
    }

    private volatile long touched;

    private void keepAlive(GwtcJobMonitor monitor, In1Out1<String, URL> getResource,  Out1Unsafe<Boolean> checkFreshness, DoUnsafe runCompile) {
        final Thread task = X_Process.newThread(() -> {
            Do undo = X_Process.scheduleInterruption(Process_TTL_Millis, TimeUnit.MILLISECONDS);
            try {
            touched = System.currentTimeMillis();
            while (System.currentTimeMillis() - touched < Process_TTL_Millis) {
                final String readValue = monitor.readAsCompiler();
                if (readValue == null) {
                    continue;
                }
                String message = readValue.trim();
                X_Log.info(GwtcJobManagerImpl.class, "Got message: ", message);
                touched = System.currentTimeMillis();
                undo.done();
                undo = X_Process.scheduleInterruption(Process_TTL_Millis, TimeUnit.MILLISECONDS);
                switch (message) {
                    case GwtcJobMonitor.JOB_COMPILE:
                    case GwtcJobMonitor.JOB_RECOMPILE:
                        runCompile.done();
                        break;
                    case GwtcJobMonitor.JOB_DIE:
                        die(monitor);
                        return;
                    case GwtcJobMonitor.JOB_CHECK_FRESHNESS:
                        final Moment start = X_Time.now();
                        boolean fresh = checkFreshness.out1();
                        final Moment freshTime = X_Time.now();
                        monitor.writeAsCompiler(Character.toString(fresh ? CompileMessage.KEY_FRESH : CompileMessage.KEY_STALE));
                        X_Log.info(GwtcJobManagerImpl.class, "Freshness check (", fresh?"passed":"failed", ") took", diff(start),
                            " time to send reply", diff(freshTime));
                        break;
                    case GwtcJobMonitor.JOB_PING:
                        break; // do nothing, we already touched our keepalive timestamp.
                    default:
                        if (message.startsWith(GwtcJobMonitor.JOB_GET_RESOURCE)) {
                            String fileName = message.substring(GwtcJobMonitor.JOB_GET_RESOURCE.length());
                            URL url = getResource.io(fileName);
                            String serFile = serializer.serializeString(fileName);
                            if (url == null) {
                                String serUrl = serializer.serializeString("");
                                if (!fileName.startsWith("gen/")) {
                                    X_Log.error(GwtcJobManagerImpl.class, "No resource found for ", fileName);
                                }
                                monitor.writeAsCompiler(CompileMessage.KEY_FOUND_RESOURCE + serFile + serUrl);
                            } else {
                                String serUrl = serializer.serializeString(url.toExternalForm());
                                monitor.writeAsCompiler(CompileMessage.KEY_FOUND_RESOURCE + serFile + serUrl);
                            }
                        } else {
                            X_Log.error(GwtcJobManagerImpl.class, "Unsupported gwt process message ", message);
                        }
                }
            }
            } catch (Throwable t) {
                final Throwable unwrapped = X_Util.unwrap(t);
                if (unwrapped instanceof InterruptedException) {
                    die(monitor);
                    return;
                }
                X_Log.error(GwtcJobManagerImpl.class, "Unexpected error in " + getClass() + ".keepAlive", unwrapped);
                if (unwrapped instanceof LinkageError || unwrapped instanceof ClassNotFoundException) {
                    die(monitor);
                }
                throw t;
            } finally {
                undo.done();
            }
        });
        task.setName(getClass().getName() +" KeepAlive");
        task.start();
    }

    /**
     * Left here for you to override any cleanup you want to do when we are told to die
     */
    protected void die(GwtcJobMonitor monitor) {
        monitor.updateCompileStatus(CompileMessage.Destroyed);
        // Tell anyone waiting that we're going away...
        callbacks.removeWhileTrue((key, callback)->{
            callback.forAll(In2::in, (CompiledDirectory)null, new JobCanceledException());
            return true;
        });
        killJobs();
        X_Log.info(GwtcJobMonitorImpl.class, "Gwtc job manager told to die", this);
    }

    private void sendCompilerResults(
        GwtcJobMonitor monitor,
        String outputModuleName,
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
            new File(dir.getExtraDir(), outputModuleName + "/symbolMaps").getAbsolutePath(),
            null,
            strategy
        );
    }

    private String path(File file) {
        return file == null ? null : file.getAbsolutePath();
    }

}

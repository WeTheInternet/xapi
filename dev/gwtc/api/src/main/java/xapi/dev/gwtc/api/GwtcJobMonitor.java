package xapi.dev.gwtc.api;

import xapi.fu.iterate.ArrayIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/12/17.
 */
public interface GwtcJobMonitor {

    // When sending arguments to our main, these are the top-level commands your arguments should start with

    /**
     * Tell Gwtc to start/recompile the module defined by the remaining arguments
     * (we parse the remaining arguments as SDM Options, and send to the recompiler instance)
     */
    String JOB_RECOMPILE = "recompile";

    /**
     * Tell Gwtc to perform a full compile on the module defined by the remaining arguments
     * (we slice off the first argument and send the rest to GwtCompiler)
     */
    String JOB_COMPILE = "compile";
    String JOB_J2CL = "j2cl";

    /**
     * Runs in test mode, sends back the rest of the arguments,
     * then forwards all input back as output.
     */
    String JOB_TEST = "test";

    /**
     * Prints a useful help message.
     */
    String JOB_HELP = "help";

    /**
     * Tell the remote process to die (and cleanup after itself).
     */
    String JOB_DIE = "die";

    /**
     * If this is the first argument, and when running as a main,
     * we will set the destination for stdOut/stdErr to the given file.
     *
     * If this argument is not set, and running as a main,
     * we will set this to a file in /tmp.
     *
     * Because we use stdOut for communication with host process,
     * you can specify magic string "1>&2" to instead just pipe stdOut to stdErr,
     * so that all output / logging goes to stdErr, and all system commands use stdOut.
     */
    String ARG_LOG_FILE = "-logFile";
    String ARG_UNIT_CACHE_DIR = "-unitCacheDir";

    /**
     * Specify "1>&2" as your -logFile argument to have all program output
     * sent through stdErr, presumably for logging.
     */
    String STD_OUT_TO_STD_ERR = "1>&2";
    String NO_LOG_FILE = "nlf";

    enum CompileStatus {
            DebugWait(CompileStatus.KEY_DEBUG_WAIT),
            Preparing(CompileStatus.KEY_PREPARING),
            Running(CompileStatus.KEY_RUNNING),
            Success(CompileStatus.KEY_SUCCESS) {
                @Override
                public boolean isComplete() {
                    return true;
                }
            },
            Log(CompileStatus.KEY_LOG),
            Ping(CompileStatus.KEY_PING),
            Failed(CompileStatus.KEY_FAILED) {
                @Override
                public boolean isComplete() {
                    return true;
                }
            }
        ;
        public static final char
            KEY_SUCCESS = 's',
            KEY_RUNNING = 'r',
            KEY_PREPARING = 'i',
            KEY_LOG = '[',
            KEY_DEBUG_WAIT = 'L',
            KEY_FAILED = 'f',
            KEY_PING = 'p'
        ;

        private final char type;

        CompileStatus(char type) {
            this.type = type;
        }

        public static CompileStatus fromChar(char c) {
            switch (c) {
                case KEY_PREPARING:
                    return Preparing;
                case KEY_RUNNING:
                    return Running;
                case KEY_SUCCESS:
                    return Success;
                case KEY_DEBUG_WAIT:
                    return DebugWait;
                case KEY_FAILED:
                    return Failed;
                case KEY_LOG:
                    return Log;
                case KEY_PING:
                    return Ping;
            }
            throw new IllegalArgumentException(c + " is not a supported status type");
        }

        public char controlChar() {
            return type;
        }

        public boolean isComplete() {
            return false;
        }
    }
    String readAsCaller();

    void writeAsCaller(String toCaller);

    String readAsCompiler();

    void writeAsCompiler(String toCompiler);

    default void updateCompileStatus(CompileStatus status, String ... args) {
        writeAsCompiler(status.type + ArrayIterable.iterate(args).join(""));
    }

    boolean hasMessageForCaller();

    boolean hasMessageForCompiler();

}

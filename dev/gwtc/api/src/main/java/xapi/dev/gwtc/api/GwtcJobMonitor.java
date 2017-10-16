package xapi.dev.gwtc.api;

import xapi.fu.In2;
import xapi.fu.iterate.ArrayIterable;
import xapi.log.X_Log;

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

    /**
     * Specify "1>&2" as your -logFile argument to have all program output
     * sent through stdErr, presumably for logging.
     */
    String STD_OUT_TO_STD_ERR = "1>&2";
    String NO_LOG_FILE = "nlf";

    enum CompileStatus {
            Preparing(CompileStatus.KEY_PREPARING),
            Running(CompileStatus.KEY_RUNNING),
            Success(CompileStatus.KEY_SUCCESS),
            Log(CompileStatus.KEY_LOG),
            Ping(CompileStatus.KEY_PING),
            Failed(CompileStatus.KEY_FAILED)
        ;
        public static final char
            KEY_SUCCESS = 's',
            KEY_RUNNING = 'r',
            KEY_PREPARING = 'i',
            KEY_LOG = '[',
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
    }
    String readAsCaller();

    void writeAsCaller(String toCaller);

    String readAsCompiler();

    void writeAsCompiler(String toCompiler);

    default void updateCompileStatus(CompileStatus status, String ... args) {
        writeAsCompiler(status.type + ArrayIterable.iterate(args).join(""));
    }

    default void readStatus(In2<CompileStatus, String> callback) {
        String response = readAsCaller().trim();
        if (response.isEmpty()) {
            X_Log.warn(GwtcJobMonitor.class, "Ignored empty response; did not call", callback);
            return;
        }
        final CompileStatus status = CompileStatus.fromChar(response.charAt(0));
        String rest = response.substring(1);
        callback.in(status, rest);
    }

    default void flushAsCaller() {

    }

    default void ping(String key) {
        writeAsCaller(CompileStatus.Ping.type + key);
    }
}

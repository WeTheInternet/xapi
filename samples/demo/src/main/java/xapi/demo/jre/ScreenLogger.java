package xapi.demo.jre;

import xapi.annotation.inject.SingletonOverride;
import xapi.demo.jre.ScreenLogger.LogSpy;
import xapi.fu.Lazy;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.log.impl.JreLog;

import java.io.OutputStream;
import java.io.PrintStream;

import static xapi.fu.itr.Chain.startChain;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/19/17.
 */
@SingletonOverride(implFor = LogService.class, priority = Integer.MAX_VALUE)
public class ScreenLogger extends JreLog {

    public interface LogSpy {
        void out(String line);
        void err(String line);
    }

    public interface LogSpyInitializer {
        LogSpy initialize(LogSpy spy);
    }

    protected static final ThreadLocal<Boolean> passThru = new ThreadLocal<>();
    /**
     * We _must_ use a Lazy here, as the method to create an instance would have to
     * run in our class initializer, and that requires `new`ing an instance before
     * said class initializer method is finished.
     */
    private static final Lazy<ScreenLogger> INSTANCE = Lazy.deferred1(ScreenLoggerInit::getInstance);

    private volatile LogSpy spy;
    protected volatile LogSpyInitializer init;

    public ScreenLogger() {
        ChainBuilder<String> out = Chain.startChain(), err = Chain.startChain();
        spy = new LogSpy() {
            @Override
            public void out(String line) {
                out.add(line);
            }

            @Override
            public void err(String line) {
                err.add(line);
            }
        };
        init = sp->{
            out.removeAll(sp::out);
            err.removeAll(sp::err);
            return sp;
        };
    }

    @Override
    protected void printLogString(LogLevel level, String logString) {
        // We don't want the print stream to decide if a message is info or error,
        // so we disable the logging spy when we are printing our message
        passThru.set(true);
        super.printLogString(level, logString);
        passThru.remove();
        // Then, we decide what goes where
        if (isFatal(level)) {
            spy.err(logString);
        } else {
            spy.out(logString);
        }
    }

    private boolean isFatal(LogLevel level) {
        return level == LogLevel.ERROR;
    }

    public static ScreenLogger getLogger() {
        return INSTANCE.out1();
    }

    public LogSpy getSpy() {
        return spy;
    }

    public void setSpy(LogSpy spy) {
        this.spy = init.initialize(spy);
    }

    protected void setSpyInitializer(LogSpyInitializer init) {
        this.init = init;
    }
}

/**
 * We do this in another class
 */
class ScreenLoggerInit {

    private static final ChainBuilder<String> cachedOuts = startChain(), cachedErrs = startChain();

    public static ScreenLogger getInstance() {
        // The work to capture system in/out is done in our class initializer,
        // to ensure this happens as early as absolutely possible.
        final LogSpy spy = new LogSpy() {
            @Override
            public void out(String line) {
                cachedOuts.add(line);
            }

            @Override
            public void err(String line) {
                cachedErrs.add(line);
            }
        };

        // It's very important that we do NOT reference the outs/errs fields in this method call;
        // we must use a lambda to defer dereferencing the field variables.
        // We will reassign the callback when the UI is ready for us (and drain any accumulated messages)
        final OutputStream outSpy = X_IO.spy(System.out, line->{
            if (!Boolean.TRUE.equals(ScreenLogger.passThru.get())) {
                ScreenLogger.getLogger().getSpy().out(line);
            }
        });
        final OutputStream errSpy = X_IO.spy(System.err, line-> {
            if (!Boolean.TRUE.equals(ScreenLogger.passThru.get())) {
                ScreenLogger.getLogger().getSpy().err(line);
            }
        });
        final PrintStream outStream = new PrintStream(outSpy, true);
        final PrintStream errStream = new PrintStream(errSpy, true);
        // Take over global variables elsewhere
        System.setOut(outStream);
        System.setErr(errStream);
        // Now that we own the system stream, call X_Inject to initialize us.
        // So long as you call into ScreenLogger before any other code tries to run injection,
        // we should beat any runtime search logs, so we can capture them for render later.
        // HINT: static initialized fields will run first, in order declared,
        // and they synchronize on the Class object (yes, you can deadlock using static field initializers)
        final LogService logger = X_Inject.singleton(LogService.class);
        assert logger instanceof ScreenLogger :
            "ScreenLogger not selected as LogService injection target; " +
                "check META-INF/singletons/" + LogService.class.getName();
        final ScreenLogger instance = (ScreenLogger) logger;
        instance.setSpy(spy);
        instance.setSpyInitializer(yourSpy->{
            if (yourSpy != spy) {
                cachedOuts.removeAll(yourSpy::out);
                cachedErrs.removeAll(yourSpy::err);
            }
            return yourSpy;
        });
        return instance;
    }
}

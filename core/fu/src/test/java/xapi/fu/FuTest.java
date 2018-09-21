package xapi.fu;

import org.junit.After;
import org.junit.Before;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.itr.SizedIterable;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A base test class with various goodies, like uncaught exception handling,
 * classloader isolation, and whatever other heavy-reuse tools we come up with.
 *
 * Less used tools will go into mixin interfaces or similar test utils.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/21/18 @ 12:07 AM.
 */
public class FuTest implements Rethrowable {

    public static boolean failed;
    private UncaughtExceptionHandler was;

    @Before
    public void before() {
        was = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler((t, e)->{
            failed = true;
            e.printStackTrace();
        });
    }

    @After
    public void after() {
        Thread.currentThread().setUncaughtExceptionHandler(was);
        if (failed) {
            failed = false; // reset for next test
            throw new AssertionError("Failed");
        }
    }

    public void borrowSout(DoUnsafe task, In1Unsafe<SizedIterable<String>> linesWritten) throws IOException {
        final Path tmp = Files.createTempFile(getClass().getName(), "borrowSout");
        final PrintStream was = System.out;
        final List<String> lines;
        try {
            final PrintStream capture = new PrintStream(tmp.toFile());
            System.setOut(capture);
            task.done();
            lines = Files.readAllLines(tmp);
            // if we succeeded, we'll cleanup; but if we fail, leave any hints...
            Files.delete(tmp);
        } finally {
            System.setOut(was);
        }
        linesWritten.in(
            SizedIterable.of(lines::size, lines)
        );
    }

}

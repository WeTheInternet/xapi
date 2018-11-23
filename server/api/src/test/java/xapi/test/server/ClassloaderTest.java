package xapi.test.server;

// There should be no imports here;
// we want to be able to load this clean into a classloader by itself.
// This message here to warn you when reviewing any diff that adds an import.

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/5/17.
 */
public class ClassloaderTest {
    public static int i = TestLambda.cnt.incrementAndGet();
    public static final TestLambda lambda = createLambda();

    private static TestLambda createLambda() {
        if ("true".equals(System.getProperty("xapi.debug", null))) {
            System.out.println("Creating lambda #" + i);
        }
        return () -> i;
    }
}

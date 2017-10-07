package xapi.test.server;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/5/17.
 */
public class ClassloaderTest {
    public static int i = TestLambda.cnt.incrementAndGet();
    public static final TestLambda lambda = createLambda();

    private static TestLambda createLambda() {
        System.out.println("Creating lambda #" + i);
        return () -> i;
    }
}

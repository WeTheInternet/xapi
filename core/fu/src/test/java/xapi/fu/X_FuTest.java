package xapi.fu;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/31/17.
 */
public class X_FuTest {


    private static class ThrowFromHere {
        public static void throwFromHere() throws Exception {
            throw new IOException("Here I am!");
        }
    }

    private static class RethrowFromHere {

        public static void rethrowFromHere() {
            try {
                ThrowFromHere.throwFromHere();
            } catch (Exception e) {
                throw X_Fu.rethrow(e);
            }
        }
    }

    @Test
    public void testSneakyThrow() {
        try {
            RethrowFromHere.rethrowFromHere();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IOException);
            final StackTraceElement[] traces = e.getStackTrace();
            assertEquals(ThrowFromHere.class.getName(), traces[0].getClassName());
            assertEquals(RethrowFromHere.class.getName(), traces[1].getClassName());
            assertEquals(X_FuTest.class.getName(), traces[2].getClassName());
        }
    }

}

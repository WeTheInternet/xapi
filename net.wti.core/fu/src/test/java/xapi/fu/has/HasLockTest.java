package xapi.fu.has;

import org.junit.Assert;
import org.junit.Test;
import xapi.fu.Mutable;
import xapi.fu.Out1;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/10/18 @ 3:26 PM.
 */
public class HasLockTest {

    Object last;

    Mutable<String> subtype = new Mutable<String>("h") {
        @Override
        public <O> O mutex(Out1<O> oOut1) {
            O o = oOut1.out1();
            last = o;
            return o;
        }
    };

    @Test
    public void testForeignClassloader() throws Throwable {
        last = "wrong";
        String success = "right";
        subtype.set(success);
        final ClassLoader cl = new URLClassLoader(new URL[]{
            HasLock.class.getProtectionDomain().getCodeSource().getLocation(),
            HasLockTest.class.getProtectionDomain().getCodeSource().getLocation(),
        }, null);
        final Class<?> clazz = cl.loadClass(TestHasLockThread.class.getName());
        final Constructor<?> ctor = clazz.getConstructor(Object.class, Object.class);
        final Object inst = ctor.newInstance(subtype, subtype);
        Thread t = (Thread) inst;

        t.setContextClassLoader(cl);
        t.start();
        t.join();

        Assert.assertEquals(success, last);
        Assert.assertEquals(success, clazz.getField("result").get(t));
        Assert.assertEquals(true, clazz.getField("foreign").getBoolean(t));
        // ensure foreign classes (don't) work as expected
        Assert.assertFalse(t instanceof TestHasLockThread);
    }
}

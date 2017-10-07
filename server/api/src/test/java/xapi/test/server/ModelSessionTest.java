package xapi.test.server;

import org.junit.Test;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.except.MultiException;
import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1.In1Unsafe;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.PrimitiveSerializerDefault;
import xapi.reflect.X_Reflect;
import xapi.server.model.ModelSession;
import xapi.source.api.CharIterator;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
public class ModelSessionTest {
    @Test
    public void testSessionDeserialization() {
        ModelSession source = X_Model.create(ModelSession.class);
        testRoundTrip(source);
        final ModelKey authKey = X_Model.newKey("auth", "key");
        source.setAuthKey(authKey);
        testRoundTrip(source);
        final ModelKey myKey = X_Model.newKey("session", "mykey");
        source.setKey(myKey);
        testRoundTrip(source);
        source.setAuthKey(null);
        testRoundTrip(source);
        source.setAuthKey(authKey);
        source.setTouched(System.currentTimeMillis());
        testRoundTrip(source);
        final StringTo<String> props = X_Collect.newStringMap(String.class);
        source.setSessionProps(props);
        testRoundTrip(source);
        props.put("one", "two");
        testRoundTrip(source);
    }

    private ModelSession deserialize(String source) {
        return X_Model.deserialize(ModelSession.class, source);
    }

    private void testRoundTrip(ModelSession source) {
        String ser = X_Model.serialize(ModelSession.class, source);
        final ModelSession deser = X_Model.deserialize(ModelSession.class, ser);
        assertEquals(source, deser);
    }

    @Test
    public void testBrokeInProd() {
        // a dumping ground the check something that breaks in prod.
        final ModelSession model = deserialize("pT3EHsessionEnTftzb}u{owf|iI_sue~iuf|t~k3Liid7nvdxb9n_gw^g{cTT Ivoid");
        System.out.println(model);
    }

    @Test
    public void testSessionKeyDeserialization() {
        String ser = "eevtne_ucf|iIpjm{{jgt_~|tACiid4vhmgqas";
        PrimitiveSerializerDefault primitives = new PrimitiveSerializerDefault();
        final CharIterator chars = CharIterator.forString(ser);
        double ts = primitives.deserializeDouble(chars);
        long rand = primitives.deserializeLong(chars);
        String iid = primitives.deserializeString(chars);
        assertFalse(chars.hasNext());
    }

    @Test
    public void testStaticInit() throws Throwable {
        TestLambda l = ()->-1;
        final URL loc = new URL("file:" + X_Reflect.getFileLoc(ClassloaderTest.class));
        TestLambda original = ClassloaderTest.lambda;
        Throwable[] failure = {null};
        final Class<TestLambda> lambdaClass = TestLambda.class;
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (failure[0] == null) {
                    failure[0] = e;
                } else {
                    if (failure[0] instanceof MultiException) {
                        e.printStackTrace();
                    } else {
                        failure[0].printStackTrace();
                        e.printStackTrace();
                    }
                    failure[0] = MultiException.mergedThrowable("Multiple failures", failure[0], e);
                }
            }
        });
        ClassLoader isolate = new URLClassLoader(new URL[]{}, null);
        Thread space = new Thread(Do.unsafe(()->{
            int[] first = {0}, second = {0}, third = {0};
            ClassLoader cl1 = new URLClassLoader(new URL[]{loc}, null);
            ClassLoader cl2 = new URLClassLoader(new URL[]{loc}, isolate);
            ClassLoader cl3 = new URLClassLoader(new URL[]{loc}, isolate);
            final In1Unsafe<int[]> task = into->{
                final ClassLoader myCl = Thread.currentThread().getContextClassLoader();
                final Class<?> cls = myCl.loadClass(ClassloaderTest.class.getName());
                into[0] = cls.getField("i").getInt(null);
                final Object lambda = cls.getField("lambda").get(null);
                assertNotEquals(original, lambda);
                assertFalse(lambdaClass.isInstance(lambda));
                assertFalse(TestLambda.class.isInstance(lambda));
                assertNotEquals(TestLambda.class.getClassLoader(), myCl);
            };
            Thread t1 = new Thread(task.provide(first).toRunnable());
            Thread t2 = new Thread(task.provide(second).toRunnable());
            Thread t3 = new Thread(task.provide(third).toRunnable());
            t1.setContextClassLoader(cl1);
            t2.setContextClassLoader(cl2);
            t3.setContextClassLoader(cl3);
            t1.start();
            t2.start();
            t3.start();
            t1.join();
            t2.join();
            t3.join();
            assertEquals(1, first[0]);
            assertEquals(1, second[0]);
            assertEquals(1, third[0]);
        }).toRunnable());

        space.setContextClassLoader(isolate);
        space.start();
        space.join();

        if (failure[0] != null) {
            throw failure[0];
        }
    }
}

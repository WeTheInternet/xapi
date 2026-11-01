package xapi.reflect.impl;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/** Real JVM method handles on proxy receivers; not an ART/device test. */
public class ReflectionServiceAndroidTest {

    public interface Parent {
        String value();
        default String inherited(final String suffix) { return value() + suffix; }
    }

    public interface Example extends Parent {
        default String greeting() { return "hello"; }
        default int add(final int left, final int right) { return left + right; }
        default String join(final String prefix, final String... values) { return prefix + String.join(",", values); }
        default void touch() { value(); }
        default void fail(final Throwable failure) throws Throwable { throw failure; }
        static String staticMethod() { return "static"; }
    }

    private final ReflectionServiceAndroid service = new ReflectionServiceAndroid();

    private Example proxy(final AtomicInteger abstractCalls) {
        return (Example) Proxy.newProxyInstance(Example.class.getClassLoader(), new Class<?>[]{Example.class},
                (instance, method, args) -> {
                    if (method.getName().equals("value")) {
                        abstractCalls.incrementAndGet();
                        return "from proxy";
                    }
                    throw new AssertionError("Default invocation recursed into proxy: " + method);
                });
    }

    @Test
    public void invokesRealDefaultOnDynamicProxyWithoutRecursion() throws Exception {
        final Example receiver = proxy(new AtomicInteger());
        assertEquals("hello", service.invokeDefaultMethod(receiver, Example.class.getMethod("greeting")));
        assertEquals("hello", service.invokeDefaultMethod(receiver, Example.class.getMethod("greeting"), (Object[]) null));
        assertEquals(7, service.invokeDefaultMethod(receiver, Example.class.getMethod("add", int.class, int.class), 3, 4));
    }

    @Test
    public void resolvesInheritedDefaultAndDispatchesAbstractAccessor() throws Exception {
        final AtomicInteger abstractCalls = new AtomicInteger();
        final Example receiver = proxy(abstractCalls);
        assertEquals("from proxy!", service.invokeDefaultMethod(receiver,
                Example.class.getMethod("inherited", String.class), "!"));
        assertEquals(1, abstractCalls.get());
    }

    @Test
    public void preservesArrayArgumentsAndVoidReturn() throws Exception {
        final AtomicInteger abstractCalls = new AtomicInteger();
        final Example receiver = proxy(abstractCalls);
        assertEquals("a:b,c", service.invokeDefaultMethod(receiver,
                Example.class.getMethod("join", String.class, String[].class), "a:", new String[]{"b", "c"}));
        assertNull(service.invokeDefaultMethod(receiver, Example.class.getMethod("touch")));
        assertEquals(1, abstractCalls.get());
    }

    @Test
    public void inheritedNoReceiverConvenienceAlsoUsesAndroidDispatch() throws Exception {
        assertEquals("hello", service.invokeDefaultMethod(Example.class.getMethod("greeting")));
    }

    @Test
    public void preservesTargetRuntimeErrorsAndCheckedThrowable() throws Exception {
        final Example receiver = proxy(new AtomicInteger());
        final Method method = Example.class.getMethod("fail", Throwable.class);
        final RuntimeException runtime = new IllegalStateException("target runtime");
        final AssertionError error = new AssertionError("target error");
        final IOException checked = new IOException("target checked");
        assertSame(runtime, failure(() -> service.invokeDefaultMethod(receiver, method, runtime)));
        assertSame(error, failure(() -> service.invokeDefaultMethod(receiver, method, error)));
        assertSame(checked, failure(() -> service.invokeDefaultMethod(receiver, method, checked)));
    }

    @Test
    public void rejectsNonDefaultForeignAndMissingInputs() throws Exception {
        final Example receiver = proxy(new AtomicInteger());
        final Method method = Example.class.getMethod("greeting");
        assertTrue(failure(() -> service.invokeDefaultMethod(new Object(), method)) instanceof IllegalArgumentException);
        assertTrue(failure(() -> service.invokeDefaultMethod((Object) null, method)) instanceof NullPointerException);
        assertTrue(failure(() -> service.invokeDefaultMethod(receiver, (Method) null)) instanceof NullPointerException);
        assertTrue(failure(() -> service.invokeDefaultMethod(receiver, Parent.class.getMethod("value"))) instanceof IllegalArgumentException);
        assertTrue(failure(() -> service.invokeDefaultMethod(receiver, Example.class.getMethod("staticMethod"))) instanceof IllegalArgumentException);
        assertTrue(failure(() -> service.invokeDefaultMethod(receiver, method, "extra")) instanceof RuntimeException);
    }

    private interface CheckedAction { void run() throws Exception; }

    private Throwable failure(final CheckedAction action) {
        try {
            action.run();
        } catch (final Throwable failure) {
            return failure;
        }
        throw new AssertionError("Expected failure");
    }
}

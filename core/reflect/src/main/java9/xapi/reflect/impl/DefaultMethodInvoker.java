package xapi.reflect.impl;

import xapi.fu.Lazy;
import xapi.util.X_Util;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/**
 * DefaultMethodInvoker:
 * <p><p>
 * This class is replaced for different versions of java,
 * to avoid having to resort to illegal, soon-to-be-impossible reflection on java internals.
 * <p><p>
 * The exact, and only reliable ABI for this class, on all platforms is {@link #invokeDefaultMethod(Object, Method, Object...)}
 * <p><p>
 * The base source is java 8 implementation, with 9+ versions packed in "MR-jar-format" (classes in META-INF/versions/N/)
 * <p> To learn more about MR-jar: https://openjdk.java.net/jeps/238
 * <p><p>
 * The reflection-per-java-version strategy is based on https://blog.jooq.org/2018/03/28/correct-reflective-access-to-interface-default-methods-in-java-8-9-10/
 * <p><p>
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 05/05/2021 @ 11:49 p.m..
 */
public class DefaultMethodInvoker {

    private static final WeakHashMap<Class, MethodHandles.Lookup> lookups = new WeakHashMap<>();

    /**
     * Uses reflection to get privileged {@link java.lang.invoke.MethodHandles.Lookup} so we can invoke default method.
     *
     * @param instance : A real of do-nothing-proxy instance of the interface w/ a default method.
     * @param method : The method to invoke.
     * @param params : Any parameters you want passed along.
     * @return The result of the method invocation, null for void.
     */
    public Object invokeDefaultMethod(Object instance, final Method method, final Object ... params) {
        try {
            // Use java9+approved "way to get a Lookup that can invoke default methods":
            final Class<?> methodOwner = method.getDeclaringClass();
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(methodOwner, MethodHandles.lookup());
            final Object value = lookup
                    .in(methodOwner)
                    .unreflectSpecial(method, method.getDeclaringClass())
                    .bindTo(instance)
                    .invokeWithArguments(params);
            return value;
        } catch (Throwable e) {
            throw X_Util.rethrow(e);
        }
    }
}

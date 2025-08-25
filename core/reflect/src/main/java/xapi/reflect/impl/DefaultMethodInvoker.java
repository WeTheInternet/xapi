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

    private static Lazy<Field> lookupField = Lazy.deferred1Unsafe(()->{
        final Field field = java.lang.invoke.MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        field.setAccessible(true);
        return field;
    });

    private static Lazy<Constructor> lookupConstructor = Lazy.deferred1Unsafe(()->{
        final Constructor ctor = java.lang.invoke.MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
        ctor.setAccessible(true);
        return ctor;
    });

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
            // Use reflection on some reflection classes to get access to a Lookup object we aren't supposed to touch :-)
            // TODO: add proper security wrapper code
            final Class<?> methodOwner = method.getDeclaringClass();
            final MethodHandles.Lookup lookup = lookups.computeIfAbsent(methodOwner, ignored-> {
                final MethodHandles.Lookup look;
                try {
                    look = (MethodHandles.Lookup) lookupConstructor.out1().newInstance(methodOwner);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot get a MethodHandlers.Lookup", e);
                }
                return look.in(methodOwner);
            });

//      try {
//          lookup = MethodHandles.lookup();
//        final Class<?> targetType = method.getDeclaringClass();
//        final Object value = lookup
//              .findSpecial(instance.getClass(), method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()), targetType)
//              .bindTo(instance)
//              .invokeWithArguments(params);
//          return value;
//      } catch (Exception e) {
//        e.printStackTrace();
            final Object value = lookup
                    .unreflectSpecial(method, method.getDeclaringClass())
                    .bindTo(instance)
                    .invokeWithArguments(params);
            return value;
        } catch (Throwable e) {
            throw X_Util.rethrow(e);
        }
    }
}

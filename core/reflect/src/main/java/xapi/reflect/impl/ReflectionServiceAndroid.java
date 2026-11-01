package xapi.reflect.impl;

import xapi.util.X_Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Opt-in Android API 30+ reflection provider using public method-handle APIs.
 * Applications must select this provider before first accessing X_Reflect.
 * There is deliberately no global injection binding.
 *
 * <p>Android's Lookup.findSpecial allows an interface implemented by the special
 * caller. The declaring interface is also the special caller, and the resulting
 * handle is bound to the receiver, including a dynamic proxy. This also supports
 * defaults inherited through another interface. In contrast, unreflectSpecial checks the
 * caller without that interface context. See Android 11's libcore
 * java.lang.invoke.MethodHandles.Lookup.findSpecial and checkSpecialCaller:
 * https://android.googlesource.com/platform/libcore/+/refs/tags/android-11.0.0_r1/ojluni/src/main/java/java/lang/invoke/MethodHandles.java
 * No hidden Lookup constructor or access override is used.
 *
 * <p>JVM tests exercise real handles but do not establish ART execution. An
 * application's Android runtime verification must include its model proxies.
 */
public class ReflectionServiceAndroid extends ReflectionServiceDefault {

    @Override
    public Object invokeDefaultMethod(final Object instance, final Method method, final Object... params) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(method, "method");
        if (!method.isDefault()) {
            throw new IllegalArgumentException("Expected an interface default method: " + method);
        }
        final Class<?> owner = method.getDeclaringClass();
        if (!owner.isInstance(instance)) {
            throw new IllegalArgumentException("Receiver does not implement " + owner.getName());
        }
        final Object[] arguments = params == null ? new Object[0] : params;
        try {
            return MethodHandles.lookup()
                    .findSpecial(owner, method.getName(),
                            MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                            owner)
                    .asFixedArity()
                    .bindTo(instance)
                    .invokeWithArguments(arguments);
        } catch (final Throwable failure) {
            throw X_Util.rethrow(failure);
        }
    }
}

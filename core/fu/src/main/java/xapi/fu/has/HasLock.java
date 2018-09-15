package xapi.fu.has;

import xapi.fu.Do;
import xapi.fu.Log;
import xapi.fu.Log.LogLevel;
import xapi.fu.Out1;
import xapi.fu.Rethrowable;
import xapi.fu.api.DoNotOverride;
import xapi.fu.iterate.LinkedIterable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/6/17.
 */
public interface HasLock {

    @DoNotOverride("Override mutex(Out1) instead")
    default void mutex(Do o) {
        mutex(o.returns1(null));
    }

    default <O> O mutex(Out1<O> o) {
        synchronized (getLock()) {
            return o.out1();
        }
    }

    default Object getLock() {
        return this;
    }

    /**
     * If the supplied source object is an instance of* HasLock,
     * (* beware use of instanceof when cross-classloader reference sharing!)
     * then the given factory callback will be invoked using said HasLock's #mutex method;
     * OTHERWISE NO SYNCHRONIZATION WILL OCCUR, NOR WILL CROSS-THREADED UPDATES CROSS MEMORY BARRIER!
     *
     * If you don't understand what this means,
     * prefer to use {@link HasLock#alwaysLock(Object, Out1)}
     *
     * AVOID CALLING THESE METHODS AS PART OF A HasLock IMPLEMENTATION, AS YOU COULD CAUSE INFINITE RECURSION
     * They are meant for non-lock-aware classes to be able to act on types that don't depend strictly on HasLock
     *
     * @param source - An instance of HasLock, or any non-null object
     * @param todo - The supplier to return a value, if any, from this method
     * @param <O> - The type of object to return.  Use <Void> and return null for non-synchronous use
     * @return
     */
    static <O> O maybeLock(Object source, Out1<O> todo) {
        if (source instanceof HasLock) {
            return ((HasLock)source).mutex(todo);
        } else if (source == null) {
            throw new NullPointerException("source");
        }

        boolean shouldReflect = "true".equals(System.getProperty("xapi.reflect.enabled", "true"));
        if (shouldReflect){
            return reflect(source, todo);
        }
        return todo.out1();
    }

    static <O> O reflect(Object source, Object todo) {
        if (source == null) {
            throw new NullPointerException("reflect.source");
        }
        if (source instanceof HasLock && todo instanceof Out1) {
            return ((HasLock) source).mutex((Out1<O>)todo);
        }
        final ClassLoader cl = source.getClass().getClassLoader();
        final Class<?> out1;
        try {
            out1 = cl.loadClass(Out1.class.getName());
        } catch (ClassNotFoundException e) {
            throw Rethrowable.firstRethrowable(source).rethrow(e);
        }
        final Method invoke;
        try {
            invoke = out1.getMethod("out1");
        } catch (NoSuchMethodException e) {
            throw Rethrowable.firstRethrowable(source).rethrow(e);
        }
        // look at this class and all of it's superclasses lists of interfaces.
        return new LinkedIterable<Class<?>>(source.getClass(), Class::getSuperclass)
            .flattenArray(Class::getInterfaces)
            .filterMapped(Class::getCanonicalName, HasLock.class.getCanonicalName()::equals)
            .firstMaybe()
            .mapIfPresent(cls -> {
                // attempt to handle foreign HasLock through reflection.
                try {
                    final Method mutex = cls.getMethod("mutex", out1);
                    final Object proxy = Proxy.newProxyInstance(cl, new Class[]{out1}, (prox, m, args) ->
                        invoke.invoke(todo)
                    );
                    final Object result = mutex.invoke(source, proxy);
                    // good luck if you do something crazy like overriding mutex(Out1) method! :-)
                    return (O) result;
                } catch (Exception e) {
                    Log.firstLog(source).log(HasLock.class, LogLevel.WARN,
                        "Unable to use foreign HasLock instance", e
                    );
                    return null;
                }
            })
            .ifAbsentSupplyUnsafe(()-> (O) invoke.invoke(todo));
    }

    /**
     * Attempts to cast to source object to HasLock and call mutex on it to resolve callback;
     * if the object is not a HasLock, it will be synchronized upon.
     *
     * If it is a HasLock from a foreign classloader, and assertions for this class are not disabled,
     * then this method will fail, to let you know you have some classloader poisoning.
     *
     * If you only want to synchronize memory barrier when the object is an instance of HasLock,
     * then prefer {@link HasLock#maybeLock(Object, Out1)}.
     *
     * AVOID CALLING THESE METHODS AS PART OF A HasLock IMPLEMENTATION, AS YOU COULD CAUSE INFINITE RECURSION
     * They are meant for non-lock-aware classes to be able to act on types that don't depend strictly on HasLock
     */
    static <O> O alwaysLock(Object source, Out1<O> todo) {
        if (source instanceof HasLock) {
            return ((HasLock)source).mutex(todo);
        }
        if (source == null) {
            synchronized (todo) {
                return todo.out1();
            }
        }

        boolean shouldReflect = "true".equals(System.getProperty("xapi.reflect.enabled", "true"));
        if (shouldReflect){
            synchronized (source) {
                return reflect(source, todo);
            }
        }

        synchronized (source) {
            return todo.out1();
        }
    }
}

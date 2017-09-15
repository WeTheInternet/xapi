package xapi.fu.has;

import xapi.fu.Do;
import xapi.fu.MapLike;
import xapi.fu.Out1;
import xapi.fu.Out2;
import xapi.fu.api.DoNotOverride;
import xapi.fu.iterate.ArrayIterable;
import xapi.fu.iterate.SizedIterable;

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
     * @param source - An instance of HasLock
     * @param todo
     * @param <O>
     * @return
     */
    static <O> O maybeLock(Object source, Out1<O> todo) {
        if (source instanceof HasLock) {
            return ((HasLock)source).mutex(todo);
        }
        assert source == null || ArrayIterable.iterate(source.getClass().getInterfaces())
            .map(Class::getCanonicalName)
            .noneMatch(HasLock.class.getCanonicalName()::equals) :
            "Failed to lock HasLock from foreign classloader; " +
                source.getClass().getCanonicalName() + " : " + source;
        return todo.out1();
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
        assert source == null || ArrayIterable.iterate(source.getClass().getInterfaces())
            .map(Class::getCanonicalName)
            .noneMatch(HasLock.class.getCanonicalName()::equals) :
            "Failed to use lock HasLock from foreign classloader; " +
                source.getClass().getCanonicalName() + " : " + source;
        synchronized (source) {
            return todo.out1();
        }
    }
}

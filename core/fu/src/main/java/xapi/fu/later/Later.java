package xapi.fu.later;

import xapi.fu.Frozen;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.fu.X_Fu;

import java.util.Map;
import java.util.WeakHashMap;

import static xapi.fu.Out1.out1Deferred;

/**
 * The XApi version of Future, Promise, Observable, etc:
 * Later -> A means to declaratively describe asynchronous processes
 * in a clean and functional manner.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/11/17.
 */
@FunctionalInterface
public interface Later <Success, Fail> {

    /**
     * Method through which to notify the Later of task completion;
     *
     *
     * @param success
     * @param fail
     * @return
     */
    void resolve(Success success, Fail fail);

    default Later <Success, Fail> succeed(Success success) {
        if (isResolved()) {
            checkNotFrozen();
        }
        Object[] memory = LaterStorage.memoize(this);
        memory[LaterStorage.INDEX_SUCCESS] = success;
        return this;
    }

    default Later <Success, Fail> fail(Fail fail) {
        if (isResolved()) {
            checkNotFrozen();
        }
        Object[] memory = LaterStorage.memoize(this);
        memory[LaterStorage.INDEX_FAILURE] = fail;
        return this;
    }

    default void checkNotFrozen() {
        if (isFrozen()) {
            throw new IllegalStateException("frozen");
        }
    }

    default boolean isFrozen() {
        return Frozen.isFrozen(this);
    }

    default Maybe<Success> getSuccess() {
        if (isResolved()) {
            Success success = LaterStorage.getSuccess(this);
            return Maybe.nullable(success);
        }
        // Hrm...  Return a blocking Maybe?  Blocking should likely be made more explicit than that;
        return Maybe.not();
    }

    default Maybe<Fail> getFail() {
        if (isResolved()) {
            final Fail fail = LaterStorage.getFail(this);
            return Maybe.nullable(fail);
        }
        return Maybe.not();
    }

    default boolean isResolved() {
        return LaterStorage.state.containsKey(this);
    }

    /**
     * Returns a "frozen" Later,
     * which is to say, a Later that closes over result state variables,
     * but which waits to actually call the SAM, {@link #resolve(Object, Object)}
     *
     * @return
     */
    default Later<Success, Fail> freeze() {
        if (this instanceof Frozen) {
            return this;
        }
        if (isResolved()) {
            return new FrozenLater<>(LaterStorage.getSuccess(this), LaterStorage.getFail(this));
        } else {
            // we want a later that will become frozen when resolved
            return new FrozenLater<>(
                out1Deferred(LaterStorage::getSuccess, this)
                , out1Deferred(LaterStorage::getFail, this)
            );
        }


    }
}

class LaterStorage {
    static final int INDEX_SUCCESS = 0;
    static final int INDEX_FAILURE = 1;
    // Always update index limit if you want other memoized storage
    static final int INDEX_LIMIT = 1;
    // TODO: get this from globalscope instead, so it is shared.
    // also TODO: enforce a "core classloader" for xapi-fu basic types,
    // so they are all implicitly safe to use across ClassWorlds (single class init, so true static semantics).
    static final Map<Later, Object[]> state = new WeakHashMap<>();

    public static <Success, Fail> Object[] memoize(Later<Success, Fail> source) {
        Object[] has = state.get(source);
        if (has == null) {
            synchronized (state) {
                // let computeIfAbsent do the double-checked locking for us :-)
                has = state.computeIfAbsent(source, s->new Object[INDEX_LIMIT]);
            }
        }
        return has;
    }

    @SuppressWarnings("unchecked")
    public static <Success> Success getSuccess(Later<Success, ?> later) {
        final Object[] val = state.get(later);
        if (val == null) {
            return null;
        }
        return (Success) val[INDEX_SUCCESS];
    }

    @SuppressWarnings("unchecked")
    public static <Fail> Fail getFail(Later<?, Fail> later) {
        final Object[] val = state.get(later);
        if (val == null) {
            return null;
        }
        return (Fail) val[INDEX_FAILURE];
    }
}

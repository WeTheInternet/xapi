package xapi.collect.api;

import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;

public interface ClassTo <V>
extends ObjectTo<Class<?>, V>
{

    default boolean containsAssignableKey(Class<?> key) {
        if (containsKey(key)) {
            return true;
        }
        for (Class<?> cls : keys()) {
            if (key.isAssignableFrom(cls)) {
                return true;
            }
        }
        return false;
    }

    default V getAssignable(Class<?> key) {
        final V value = get(key);
        if (value != null) {
            return value;
        }
        for (Class<?> cls : keys()) {
            if (key.isAssignableFrom(cls)) {
                return get(cls);
            }
        }
        return null;
    }

    default <T, Generic extends T> MappedIterable<V> iterateAssignableKey(Class<Generic> key) {
        ChainBuilder<V> chain = Chain.startChain();
        forEachAssignableKey(key, chain::add);
        return chain;
    }

    default <T extends V, Generic extends T> MappedIterable<V> iterateAssignableValue(Class<Generic> key) {
        ChainBuilder<V> chain = Chain.startChain();
        forEachAssignableValue(key, chain::add);
        return chain;
    }

    default <T> void forEachAssignableKey(Class<? super T> key, In1<V> callback) {
        for (Class<?> cls : keys()) {
            if (key.isAssignableFrom(cls)) {
                final V value = get(cls);
                callback.in(value);
            }
        }
    }

    default <T extends V> void forEachAssignableValue(Class<? super T> key, In1<V> callback) {
        for (Class<?> cls : keys()) {
            final V value = get(cls);
            if (key.isInstance(value)) {
                callback.in(value);
            }
        }
    }

    default Maybe<V> findAssignableKeyOrCompute(Class<?> key, In1Out1<Class<?>, V> factory) {
        final Maybe<V> maybe = firstWhereKey(key::isAssignableFrom);
        if (maybe.isPresent()) {
            return maybe;
        }
        final V value = factory.io(key);
        if (value == null) {
            return Maybe.nullable(value);
        } else {
            put(key, value);
            return Maybe.immutable(value);
        }
    }

    interface Many <V>
    extends ClassTo<IntTo<V>>, ObjectTo.Many<Class<?>, V>
  {}

}

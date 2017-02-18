package xapi.fu;

import xapi.fu.In1.In1Unsafe;

import java.util.Optional;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/4/16.
 */
public interface Maybe <V> extends Rethrowable {

    Maybe NULL = ()->null;

    V get();

    class ImmutableMaybe<V> implements Maybe<V>, IsImmutable {

        private final V value;

        public ImmutableMaybe(V value) {
            this.value = value;
        }

        @Override
        public V get() {
            return value;
        }
    }

    default boolean isPresent() {
        return get() != null;
    }

    default boolean isAbsent() {
        return get() == null;
    }

    default V getOrThrow() {
        return getOrThrow(IllegalStateException::new);
    }

    default V getOrThrow(Out1<Throwable> factory) {
        if (isAbsent()) {
            throw rethrow(factory.out1());
        }
        return get();
    }

    default <O> ImmutableMaybe<O> mapImmediate(In1Out1<V, O> mapper) {
        V value = get();
        O mapped = mapper.io(value);
        return Maybe.immutable(mapped);
    }

    default <O> Maybe<O> mapDeferred(In1Out1<V, O> mapper) {
        return ()->{
            V value = get();
            @SuppressWarnings("all")
            O mapped = mapper.io(value);
            return mapped;
        };
    }

    static <O> Maybe<O> nullable(O mapped) {
        return mapped == null ? not() : immutable(mapped);
    }
    static <O> ImmutableMaybe<O> immutable(O mapped) {
        return new ImmutableMaybe<>(mapped);
    }

    default <O> Maybe<O> mapNullSafe(In1Out1<V, O> mapper) {
        return ()-> {
            V out = get();
            if (out == null) {
                return null;
            }
            return mapper.io(out);
        };
    }

    default <O> Maybe<O> mapIfPresent(In1Out1<V, O> mapper, Out1<Throwable> factory) {
        return ()-> {
            V out = get();
            if (out == null) {
                final Throwable throwable = factory.out1();
                if (throwable == null) {
                    return null;
                }
                throw rethrow(throwable);
            }
            return mapper.io(out);
        };
    }

    default <O> Maybe<O> mapIfPresent(In1Out1<V, O> mapper) {
        return mapIfPresent(mapper, Out1.null1());
    }

    default Optional<V> optional() {
        if (isAbsent()) {
            return Optional.empty();
        }
        return Optional.of(get());
    }

    static <V> Maybe<V> not() {
        return NULL;
    }

    default Maybe<V> readIfPresentUnsafe(In1Unsafe<V> val) {
        return readIfPresent(val);
    }
    default Maybe<V> readIfPresent(In1<V> val) {
        if (isPresent()) {
            val.in(get());
        }
        return this;
    }

    default Maybe<V> readIfAbsentUnsafe(In1Unsafe<V> val) {
        return readIfAbsent(val);
    }
    default Maybe<V> readIfAbsent(In1<V> val) {
        if (!isPresent()) {
            val.in(get());
        }
        return this;
    }
    default <I1, I2> V ifAbsentSupply(In2Out1<I1, I2, V> val, I1 i1, I2 i2) {
        if (isPresent()) {
            return get();
        }
        return val.io(i1, i2);
    }
    default <I1, I2> V ifAbsentSupplyLazy(In2Out1<I1, I2, V> val, Out1<I1> i1, Out1<I2> i2) {
        if (isPresent()) {
            return get();
        }
        return val.io(i1.out1(), i2.out1());
    }

    default <I1> V ifAbsentSupply(In1Out1<I1, V> val, I1 i1) {
        if (isPresent()) {
            return get();
        }
        return val.io(i1);
    }
    default <I1> V ifAbsentSupplyLazy(In1Out1<I1, V> val, Out1<I1> i1) {
        if (isPresent()) {
            return get();
        }
        return val.io(i1.out1());
    }
    default V ifAbsentSupply(Out1<V> val) {
        if (isPresent()) {
            return get();
        }
        return val.out1();
    }
    default V ifAbsentReturn(V val) {
        if (isPresent()) {
            return get();
        }
        return val;
    }

}

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

    default boolean isPresentAndMatches(In1Out1<V, Boolean> filter) {
        return isPresent() && filter.io(get());
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

    default <A, O> Maybe<O> mapNullSafe(In2Out1<V, A, O> mapper, A also) {
        return ()-> {
            V out = get();
            if (out == null) {
                return null;
            }
            return mapper.io(out, also);
        };
    }

    default <A, B, O> Maybe<O> mapNullSafe(In3Out1<V, A, B, O> mapper, A also, B balso) {
        return ()-> {
            V out = get();
            if (out == null) {
                return null;
            }
            return mapper.io(out, also, balso);
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
    default <O, E1> Maybe<O> mapIfPresent(In2Out1<V, E1, O> mapper, E1 extra) {
        return mapIfPresent(mapper.supply2(extra));
    }
    default <O, E1> Maybe<O> mapIfPresentLazy(In2Out1<V, E1, O> mapper, Out1<E1> extra) {
        return mapIfPresent(mapper.supply2Deferred(extra));
    }
    default <O, E1, E2> Maybe<O> mapIfPresent(In3Out1<V, E1, E2, O> mapper, E1 extra1, E2 extra2) {
        return mapIfPresent(mapper.supply2(extra1).supply2(extra2));
    }
    default <O, E1, E2> Maybe<O> mapIfPresentLazy(In3Out1<V, E1, E2, O> mapper, Out1<E1> extra1, Out1<E2> extra2) {
        return mapIfPresent(mapper.supply2Deferred(extra1).supply2Deferred(extra2));
    }

    default Maybe<V> mapIfAbsent(Out1<V> supplier) {
        return ()->{
            final V out = get();
            if (out == null) {
                return supplier.out1();
            }
            return out;
        };
    }

    default <I1> Maybe<V> mapIfAbsent(In1Out1<I1, V> supplier, I1 i1) {
        return ()->{
            final V out = get();
            if (out == null) {
                return supplier.io(i1);
            }
            return out;
        };
    }
    default <I1> Maybe<V> mapIfAbsentLazy(In1Out1<I1, V> supplier, Out1<I1> i1) {
        return ()->{
            final V out = get();
            if (out == null) {
                return supplier.io(i1.out1());
            }
            return out;
        };

    }

    default <I1, I2> Maybe<V> mapIfAbsent(In2Out1<I1, I2, V> supplier, I1 i1, I2 i2) {
        return ()->{
            final V out = get();
            if (out == null) {
                return supplier.io(i1, i2);
            }
            return out;
        };
    }
    default <I1, I2> Maybe<V> mapIfAbsentLazy(In2Out1<I1, I2, V> supplier, Out1<I1> i1, Out1<I2> i2) {
        return ()->{
            final V out = get();
            if (out == null) {
                return supplier.io(i1.out1(), i2.out1());
            }
            return out;
        };

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
    default <I1> Maybe<V> readIfPresent1(In2<I1, V> val, I1 i1) {
        return readIfPresent(val.provide1(i1));
    }
    default <I2> Maybe<V> readIfPresent2(In2<V, I2> val, I2 i2) {
        return readIfPresent(val.provide2(i2));
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
    default <I1, I2, I3> V ifAbsentSupply(In3Out1<I1, I2, I3, V> val, I1 i1, I2 i2, I3 i3) {
        if (isPresent()) {
            return get();
        }
        return val.io(i1, i2, i3);
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

    default Maybe<V> filter(In1Out1<V, Boolean> filter) {
        return ()->{
            if (isPresent()) {
                final V val = get();
                if (filter.io(val)) {
                    return val;
                }
            }
            return null;
        };
    }

    default <A> Maybe<V> filter(In2Out1<V, A, Boolean> filter, A also) {
        return ()->{
            if (isPresent()) {
                final V val = get();
                if (filter.io(val, also)) {
                    return val;
                }
            }
            return null;
        };
    }

    default <A, B> Maybe<V> filter(In3Out1<V, A, B, Boolean> filter, A also, B balso) {
        return ()->{
            if (isPresent()) {
                final V val = get();
                if (filter.io(val, also, balso)) {
                    return val;
                }
            }
            return null;
        };
    }

    @SuppressWarnings("unchecked")
    default Maybe<V> lazy() {
        final Out1[] value = {this::get};
        return ()->{
            synchronized (value) {
                if (!(value[0] instanceof IsImmutable)) {
                    final Object val = value[0].out1();
                    if (val != null) {
                        value[0] = Immutable.immutable1(val);
                    }
                    return (V)val;
                }
            }
            return (V)value[0].out1();
        };
    }
}

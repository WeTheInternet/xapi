package xapi.fu;

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

    default <O> Maybe<O> mapIfNotNull(In1Out1<V, O> mapper, In1Out1<V, Throwable> factory) {
        return ()-> {
            V out = get();
            if (out == null) {
                final Throwable throwable = factory.io(get());
                throw rethrow(throwable);
            }
            return mapper.io(out);
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
}

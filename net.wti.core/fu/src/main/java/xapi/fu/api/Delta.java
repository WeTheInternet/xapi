package xapi.fu.api;

import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.IsImmutable;
import xapi.fu.IsMutable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/19/17.
 */
public interface Delta<T> {

    T getBefore();

    T getAfter();

    static <T> ImmutableDelta<T> immutable(T before, T after) {
        return new ImmutableDelta<>(before, after);
    }
    static <T> MutableDelta<T> mutable(T before, T after) {
        return new MutableDelta<>(before, after);
    }
    static <T> MutableDelta<T> mutable(T before) {
        return new MutableDelta<>(before, null);
    }

    final class ImmutableDelta<T> implements Delta<T>, IsImmutable {

        private final T before;
        private final T after;

        public ImmutableDelta(T before, T after) {
            this.before = before;
            this.after = after;
        }

        @Override
        public T getBefore() {
            return before;
        }

        @Override
        public T getAfter() {
            return after;
        }

    }
    final class MutableDelta<T> implements Delta<T>, IsMutable {

        private T before;
        private T after;

        public MutableDelta() {
        }
        public MutableDelta(T before, T after) {
            this.before = before;
            this.after = after;
        }

        @Override
        public T getBefore() {
            return before;
        }

        @Override
        public T getAfter() {
            return after;
        }

        public void setBefore(T before) {
            this.before = before;
        }

        public void setAfter(T after) {
            this.after = after;
        }
    }

    class DeltaFactory<T> {

        private Delta<T> delta;

        public DeltaFactory(T seed) {
            delta = Delta.immutable(null, seed);
        }

        public Delta<T> nextDelta(T next) {
            delta = Delta.immutable(delta.getAfter(), next);
            return delta;
        }

        public Delta<T> nextDelta(In1Out1<T, T> mapper) {
            final T current = delta.getAfter();
            final T next = mapper.io(current);
            delta = Delta.immutable(current, next);
            return delta;
        }

        /**
         * Provided in case you have a method reference you want to listen in on
         * void setValue(T myVal) {
         * delta = factory.nextDelta(myVal, this::onChange);
         * }
         *
         * T onChange(T before, T after) {
         *     // do nefarious things
         * }
         *
         */
        public Delta<T> nextDelta(T next, In2Out1<T, T, T> mapper) {
            final T current = delta.getAfter();
            final T result = mapper.io(current, next);
            delta = Delta.immutable(current, result);
            return delta;
        }

    }
}

package xapi.fu;

import xapi.fu.Filter.Filter1;
import xapi.fu.iterate.ArrayIterable;
import xapi.fu.iterate.CachingIterator;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.fu.iterate.EmptyIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public interface MappedIterable<T> extends Iterable<T> {

    default <To> MappedIterable<To> map(In1Out1<T, To> mapper) {
        return mapIterable(this, mapper);
    }

    default MappedIterable<T> spy(In1<T> mapper) {
        return mapIterable(this, mapper.returnArg());
    }

    default ChainBuilder<T> copy() {
        ChainBuilder<T> copy = Chain.startChain();
        copy.addAll(this);
        return copy;
    }

    default <To> MappedIterable<To> flatten(In1Out1<T, Iterable<To>> mapper) {
        return () -> new Iterator<To>() {

            final Iterator<Iterable<To>> itr = map(mapper).iterator();

            Lazy<Iterator<To>> next = reset();

            private Lazy<Iterator<To>> reset() {
                return Lazy.deferred1(()->{
                    if (itr.hasNext()) {
                        final Iterable<To> n = itr.next();
                        return n.iterator();
                    }
                    return EmptyIterator.empty();
                });
            }

            @Override
            public boolean hasNext() {
                final Iterator<To> current = next.out1();
                if (current.hasNext()) {
                    return true;
                }

                return false;
            }

            @Override
            public To next() {
                synchronized (itr) {
                    final Iterator<To> current = next.out1();
                    try {
                        return current.next();
                    } finally {
                        if (!current.hasNext()) {
                            next = reset();
                        }
                    }
                }
            }
        };
    }

    static <To> MappedIterable<To> mapped(Iterable<To> itr) {
        return itr instanceof MappedIterable ? (MappedIterable<To>) itr : itr::iterator;
    }

    static <To> MappedIterable<To> mapped(To ... items) {
        return new ArrayIterable<>(items);
    }

    static <From, To> MappedIterable<To> mapIterable(Iterable<From> from, In1Out1<? super From, ? extends To> mapper) {
        return ()->new MappedIterator<>(from.iterator(), mapper);
    }

    default boolean hasMatch(Filter1<T> filter) {
        return firstMatch(filter).isPresent();
    }

    default Maybe<T> firstMatch(Filter1<T> filter) {
        for (T t:this) {
            if (filter.filter1(t)) {
                return Maybe.immutable(t);
            }
        }
        return Maybe.not();
    }

    default MappedIterable<T> filter(Filter1<T> filter) {
        return ()->new Iterator<T>() {
            Iterator<T> iter = iterator();
            Lazy<T> next = reset();

            private Lazy<T> reset() {
                return Lazy.deferred1(()->{
                    assert iter != null;
                    while (iter.hasNext()) {
                        final T n = iter.next();
                        if (filter.filter1(n)) {
                            return n;
                        }
                    }
                    return null;
                });
            }

            @Override
            public boolean hasNext() {
                next.out1();
                return next.isFull1();
            }

            @Override
            public T next() {
                try {
                    return next.out1();
                } finally {
                    next = reset();
                }
            }
        };
    }

    default T reduce(In2Out1<T, T, T> reducer, T seed) {
        for (T t : this) {
            seed = reducer.io(seed, t);
        }
        return seed;
    }

    default <O> O reduceInstances(In2Out1<T, O, O> reducer, O seed) {
        for (T t : this) {
            seed = reducer.io(t, seed);
        }
        return seed;
    }

    default T reduceWhile(Filter1<T> filter, In2Out1<T, T, T> reducer, T seed) {
        for (T t : this) {
            if (filter.filter1(t)) {
                seed = reducer.io(seed, t);
            } else {
                return seed;
            }
        }
        return seed;
    }

    default T first() {
        final Iterator<T> itr = iterator();
        if (itr.hasNext()) {
            return itr.next();
        }
        throw new NoSuchElementException();
    }

    default T firstOrNull() {
        final Iterator<T> itr = iterator();
        if (itr.hasNext()) {
            return itr.next();
        }
        return null;
    }

    default T last() {
        final Iterator<T> itr = iterator();
        if (!itr.hasNext()) {
           throw new NoSuchElementException();
        }
        T next = null;
        while (itr.hasNext()) {
            next = itr.next();
        }
        return next;
    }

    default String join(String before, String joiner, String after) {
        return before + join(joiner) + after;
    }
    default String join(String joiner) {
        return join(Object::toString, joiner);
    }

    default String join(In1Out1<T, String> toString, String joiner) {
        final Iterator<T> itr = iterator();
        if (!itr.hasNext()) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        String next = toString.io(itr.next());
        b.append(next);
        while (itr.hasNext()) {
            b.append(joiner).append(itr.next());
        }
        return b.toString();
    }

    default boolean itemsEquals(Iterable<T> other) {
        return X_Fu.iterEqual(this, other);
    }

    default <To> boolean itemsEqualsMapped(In1Out1<T, To> mapper, Iterable<T> other) {
        return X_Fu.iterEqual(this.map(mapper), MappedIterable.mapped(other).map(mapper));
    }

    default boolean anyMatch(Filter1<T> filter) {
        final Iterator<T> itr = iterator();
        while (itr.hasNext()) {
            if (filter.filter1(itr.next())) {
                return true;
            }
        }
        return false;
    }

    default boolean noneMatch(Filter1<T> filter) {
        final Iterator<T> itr = iterator();
        while (itr.hasNext()) {
            if (filter.filter1(itr.next())) {
                return false;
            }
        }
        return true;
    }
    default MappedIterable<T> forAll(In1<T> consumer) {
        forEach(consumer.toConsumer());
        return this;
    }

    default <S> MappedIterable<T> forAll(In2<T, S> consumer, S constant) {
        forEach(consumer.provide2(constant).toConsumer());
        return this;
    }

    default <S1, S2> MappedIterable<T> forAll(In3<T, S1, S2> consumer, S1 const1, S2 const2) {
        forEach(consumer.provide2(const1).provide2(const2).toConsumer());
        return this;
    }

    default <S1, S2, S3> MappedIterable<T> forAll(In4<T, S1, S2, S3> consumer, S1 const1, S2 const2, S3 const3) {
        forEach(consumer.provide2(const1).provide2(const2).provide2(const3).toConsumer());
        return this;
    }

    default boolean isNotEmpty() {
        return iterator().hasNext();
    }
    default boolean isEmpty() {
        return !iterator().hasNext();
    }

    default <O> MappedIterable<O> ifNotEmpty(In1Out1<T, O> mapper) {
        return map(i->i==null?null:mapper.io(i));
    }

    /**
     * @return An iterable which caches as it goes, but does not read all items immediately.
     *
     * Good for situations when you want to create things lazily.
     */
    default MappedIterable<T> caching() {
        return CachingIterator.cachingIterable(iterator());
    }

    /**
     * @return A complete copy of this iterable (performs a full iteration to prime cache).
     *
     * Good for situations when you want to capture then clear something stateful,
     * (you want to read the full payload of a list, so you do not see concurrent mutations).
     *
     */
    default MappedIterable<T> cached() {
        final MappedIterable<T> itr = CachingIterator.cachingIterable(iterator());
        // Read the backing iterable into our cache.
        itr.forAll(In1.ignored());
        return itr;
    }
}

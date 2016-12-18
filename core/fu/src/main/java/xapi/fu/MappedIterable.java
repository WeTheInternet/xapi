package xapi.fu;

import xapi.fu.Filter.Filter1;
import xapi.fu.iterate.ArrayIterable;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.fu.iterate.EmptyIterator;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public interface MappedIterable<T> extends Iterable<T> {

    default <To> MappedIterable<To> map(In1Out1<T, To> mapper) {
        return mapIterable(this, mapper);
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

    default MappedIterable<T> filter(Filter1<T> filter) {
        return ()->new Iterator<T>() {
            Iterator<T> iter = iterator();
            Lazy<T> next = reset();

            private Lazy<T> reset() {
                return Lazy.deferred1(()->{
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
        return iterator().next();
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
}

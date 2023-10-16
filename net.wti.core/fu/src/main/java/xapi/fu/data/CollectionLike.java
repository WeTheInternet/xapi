package xapi.fu.data;

import xapi.fu.Filter.Filter1;
import xapi.fu.Maybe;
import xapi.fu.Out2;
import xapi.fu.X_Fu;
import xapi.fu.api.Clearable;
import xapi.fu.api.Ignore;
import xapi.fu.has.HasItems;
import xapi.fu.has.HasLock;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.MappedIterator;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.SizedIterator;

import java.util.Iterator;
import java.util.concurrent.locks.Lock;

/**
 * Lists, Sets AND Maps all implement CollectionLike (with maps using Out2 tuples of K:V for standard iteration).
 *
 * You should strive to override as many methods as you can with more performant overrides.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 4:44 AM.
 */
@Ignore("model")
public interface CollectionLike <V> extends Clearable, SizedIterable<V>, HasItems<V>, MappedIterable<V> {

    @Override
    default SizedIterable<V> forEachItem() {
        return this;
    }

    CollectionLike<V> add(V value);

    default <N> SizedIterable<Out2<V, N>> merge(boolean shrink, CollectionLike<N> other) {
        final Object lock = this;
        return SizedIterable.of(
            ()-> shrink ? Math.min(size(), other.size()) : Math.max(size(), other.size()),
            ()-> {
                final SizedIterator<V> me = iterator();
                final SizedIterator<N> you = other.iterator();
                return new Iterator<Out2<V, N>>() {
                    @Override
                    public boolean hasNext() {
                        return me.hasNext() ? !shrink || you.hasNext() : !shrink && you.hasNext();
                    }

                    @Override
                    public Out2<V, N> next() {
                        return Out2.out2Immutable(
                            me.hasNext() ? me.next() : null,
                            you.hasNext() ? you.next() : null
                        );
                    }
                };
            });
    }

    default CollectionLike<V> addNow(Iterable<? extends V> items) {
        return HasLock.maybeLock(this, ()->{
            items.forEach(this::add);
            return this;
        });
    }

    default SizedIterable<V> clearItems() {
        return HasLock.maybeLock(this, ()-> {
            final SizedIterable<V> all = cached();
            clear();
            return all;
        });
    }

    default V removeFirst() {
        return HasLock.maybeLock(this, ()-> {
            final SizedIterator<V> itr = iterator();
            if (itr.hasNext()) {
                final V next = itr.next();
                itr.remove();
                return next;
            }
            throw new IllegalStateException("Called removeFirst on an empty " + getClass() + ". Instead use removeFirstMaybe");
        });
    }
    default Maybe<V> removeFirstMaybe() {
        return HasLock.maybeLock(this, ()-> {
            final SizedIterator<V> itr = iterator();
            if (itr.hasNext()) {
                final V next = itr.next();
                itr.remove();
                return Maybe.immutable(next);
            }
            return Maybe.not();
        });
    }

    default SizedIterable<V> removeMatches(Filter1<V> filter) {
        Object lock = this;
        MappedIterable<V> i = ()-> {
            final SizedIterator<V> itr = CollectionLike.this.iterator();
            return new Iterator<V>() {
                private V nextVal;

                @Override
                public boolean hasNext() {
                    return HasLock.maybeLock(lock, ()-> {
                        while (nextVal == null) {
                            if (!itr.hasNext()) {
                                return false;
                            }
                            nextVal = itr.next();
                            if (filter.io(nextVal)) {
                                return true;
                            } else {
                                nextVal = null;
                            }

                        }
                        return false;
                    });
                }

                @Override
                public V next() {
                    return HasLock.maybeLock(lock, ()->{
                        try {
                            return nextVal;
                        } finally {
                            itr.remove();
                            nextVal = null;
                        }
                    });
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Called by next()");
                }
            };
        };
        return i.cached();
    }
    default boolean removeIf(Filter1<V> filter) {
        return HasLock.maybeLock(this, ()->{
            boolean removed = false;
            for (
                final SizedIterator<V> itr = iterator();
                itr.hasNext();
            ) {
                final V next = itr.next();
                if (filter.filter1(next)) {
                    removed = true;
                    itr.remove();
                }
            }
            return removed;
        });
    }

    default boolean removeAllEquality(CollectionLike<V> others) {
        return removeIf(others::containsEquality);
    }

    default boolean removeAllReference(CollectionLike<V> others) {
        return removeIf(others::containsReference);
    }

}

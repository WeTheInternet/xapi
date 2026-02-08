package xapi.fu.itr;

import xapi.fu.In1Out1;
import xapi.fu.IsImmutable;
import xapi.fu.Out1;
import xapi.fu.has.HasItems;
import xapi.fu.has.HasSize;
import xapi.fu.itr.CachingIterator.ReplayableIterable;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/5/17.
 */
public interface SizedIterator<T> extends Iterator<T>, HasSize, HasItems {

    static <T> SizedIterator<T> of(Out1<Integer> size, Iterator<T> result) {
        return of(size.out1(), result);
    }

    static <T> SizedIterator<T> of(int size, Iterator<T> result) {
        if (result instanceof SizedIterator) {
            return (SizedIterator<T>) result;
        }
        return new SizedIterator<T>() {

            int remaining = size;

            @Override
            public boolean hasNext() {
                return result.hasNext();
            }

            @Override
            public T next() {
                remaining--;
                assert remaining >= 0;
                return result.next();
            }

            @Override
            public int size() {
                return remaining;
            }

            @Override
            public void remove() {
                result.remove();
            }
        };
    }

    static <F, T> SizedIterator<T> of(Iterator<F> source, Out1<Integer> size, In1Out1<F, T> mapper) {
        if (source instanceof SizedIterator) {
            return of((SizedIterator<F>)source, mapper);
        }
        return new SizedIterator<T>() {
            public Boolean sizeLocked;
            int used, removed;
            @Override
            public boolean hasNext() {
                return source.hasNext();
            }

            @Override
            public T next() {
                used ++;
                final F next = source.next();
                final T result = mapper.io(next);
                return result;
            }

            @Override
            public void remove() {
                removed ++;
                if (sizeLocked == null) {
                    // only test sizeLock once, as size() might be expensive
                    int s = size.out1();
                    source.remove();
                    if (s == size.out1()) {
                        sizeLocked = true;
                    }
                } else {
                    source.remove();
                }
            }

            @Override
            public int size() {
                if (Boolean.TRUE.equals(sizeLocked)) {
                    // the source size factory is not being affected by removals,
                    // so we don't fix our remaining size by our amount removed
                    return size.out1() - used;
                }
                // the source size is presumed (or proven) to be adjusted by removal,
                // so we assume our iterator size = originalSize - usedItems;
                // we add # removed to compensate for source size computation
                return size.out1() + removed - used;
            }
        };
    }

    static <F, T> SizedIterator<T> of(SizedIterator<F> source, In1Out1<F, T> mapper) {
        return new SizedIterator<T>() {
            @Override
            public boolean hasNext() {
                return source.hasNext();
            }

            @Override
            public T next() {
                return mapper.io(source.next());
            }

            @Override
            public int size() {
                return source.size();
            }

            @Override
            public void remove() {
                source.remove();
            }
        };
    }

    default SizedIterator<T> readOnly() {
        return new ImmutableSizedIterator<>(this);
    }

    /**
     * Turns this iterator back into an iterable, for convenience.
     *
     * Note, calling ArrayIterable.once(someArray).itr() is wasteful,
     * once() just returns the iterator of an iterable it already had in memory.
     *
     */
    default SizedIterable<T> itr() {
        return SizedIterable.of(size(), MappedIterable.mappedOneShot(this));
    }

    /**
     * @return A caching iterable over the elements in this iterator.
     * If you use this method, or the fact we implement HasItems,
     * you should not also be accessing this object like it was an iterator;
     * each item we return can only be seen once, so make sure you use this iterable entirely.
     *
     * Even though we are a one-shot item, we can return an object that can be reused.
     * Obviously not going to work well if you also use this iterator instance;
     * i.e. if you use ArrayIterable.once(someArray); use that iterator,
     * but also try to use the iterable created here,
     * you are
     */
    @Override
    default ReplayableIterable<T> forEachItem() {
        return MappedIterable.mappedCaching(this);
    }
}

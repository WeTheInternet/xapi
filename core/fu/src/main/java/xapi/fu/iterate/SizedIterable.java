package xapi.fu.iterate;

import xapi.fu.Immutable;
import xapi.fu.In1Out1;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.api.DoNotOverride;
import xapi.fu.has.HasSize;

import java.util.Iterator;

import static xapi.fu.iterate.SingletonIterator.singleItem;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/5/17.
 */
public interface SizedIterable <T> extends MappedIterable<T>, HasSize {

    @Override
    SizedIterator<T> iterator();

    static <T> SizedIterable<T> of(int size, Iterable<T> itr) {
        return of(Immutable.immutable1(size), itr);
    }

    @Override
    default SizedIterable<T> counted() {
        // optimization.  If you want to eagerly process items, do .cached().counted()
        return this;
    }

    static <T> SizedIterable<T> of(Out1<Integer> size, Iterable<T> itr) {
        return new SizedIterable<T>() {
            @Override
            public SizedIterator<T> iterator() {
                final Iterator<T> result = itr.iterator();
                if (result instanceof SizedIterator) {
                    return (SizedIterator<T>) result;
                } else {
                    return SizedIterator.of(size(), result);
                }
            }

            @Override
            public int size() {
                return size.out1();
            }
        };
    }

    static <F, T> SizedIterable<T> of(SizedIterable<F> itr, In1Out1<F, T> mapper) {
        return of(itr::size, itr, mapper);
    }

    static <F, T> SizedIterable<T> of(Out1<Integer> size, Iterable<F> itr, In1Out1<F, T> mapper) {
        return new SizedIterable<T>() {
            @Override
            public SizedIterator<T> iterator() {
                final Iterator<F> result = itr.iterator();
                final SizedIterator<F> source;
                if (result instanceof SizedIterator) {
                    source = ((SizedIterator<F>) result);
                } else {
                    source = SizedIterator.of(size(), result);
                }
                return SizedIterator.of(source, mapper);
            }

            @Override
            public int size() {
                return size.out1();
            }
        };
    }

    default SizedIterable<T> mergePrepend(T valueToInsert) {
        return mergePrepend(singleItem(valueToInsert));
    }

    default SizedIterable<T> mergePrepend(SizedIterable<T> valueToInsert) {
        if (valueToInsert == null) {
            return this;
        }
        return new JoinedSizedIterable<>(valueToInsert, this);
    }

    @Override
    default <To> SizedIterable<To> map(In1Out1<T, To> mapper) {
        return adaptSizedIterable(this, mapper);
    }

    @Override
    @DoNotOverride
    default <To> SizedIterable<To> mapUnsafe(In1Out1Unsafe<T, To> mapper) {
        return map(mapper);
    }

    static <From, To> SizedIterable<To> adaptSizedIterable(SizedIterable<From> from, In1Out1<? super From, ? extends To> mapper) {
        return new SizedIterable<To>() {
            @Override
            public SizedIterator<To> iterator() {
                return new MappedSizedIterator<>(from.iterator(), mapper);
            }

            @Override
            public int size() {
                return from.size();
            }
        };
    }

    default SizedIterable<T> mergeAppend(T valueToInsert) {
        return mergeAppend(singleItem(valueToInsert));
    }

    default SizedIterable<T> mergeAppend(SizedIterable<T> valueToInsert) {
        if (valueToInsert == null) {
            return this;
        }
        return new JoinedSizedIterable<>(this, valueToInsert);
    }

    default SizedIterable<T> mergeInsert(int i, T valueToInsert) {
        return mergeInsert(i, singleItem(valueToInsert));
    }

    default SizedIterable<T> mergeInsert(int i, SizedIterable<T> valueToInsert) {
        if (i == 0) {
            // prepend
            return mergePrepend(valueToInsert);
        }
        if (i == -1) {
            // append
            return mergeAppend(valueToInsert);
        }
        if (i < -1) {
            throw new IllegalArgumentException("negative index " + i + " disallowed; only -1 is magic (for append)");
        }
        // slice this iterable at the given boundary
        return new SlicedSizedIterable<>(
            i, this,
            SlicedSizedIterable.DRAIN_ALL, valueToInsert,
            true
        );
    }

    @Override
    default boolean isEmpty() {
        return MappedIterable.super.isEmpty();
    }

    @Override
    default boolean isNotEmpty() {
        return MappedIterable.super.isNotEmpty();
    }

}

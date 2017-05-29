package xapi.fu.iterate;

import xapi.fu.Immutable;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;
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

    default SizedIterable<T> prepend(T valueToInsert) {
        return prepend(singleItem(valueToInsert));
    }

    default SizedIterable<T> prepend(SizedIterable<T> valueToInsert) {
        if (valueToInsert == null) {
            return this;
        }
        return new BiSizedIterable<>(valueToInsert, this);
    }

    default SizedIterable<T> append(T valueToInsert) {
        return append(singleItem(valueToInsert));
    }

    default SizedIterable<T> append(SizedIterable<T> valueToInsert) {
        if (valueToInsert == null) {
            return this;
        }
        return new BiSizedIterable<>(this, valueToInsert);
    }

    default SizedIterable<T> insert(int i, T valueToInsert) {
        return insert(i, singleItem(valueToInsert));
    }

    default SizedIterable<T> insert(int i, SizedIterable<T> valueToInsert) {
        if (i == 0) {
            // prepend
            return prepend(valueToInsert);
        }
        if (i == -1) {
            // append
            return append(valueToInsert);
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
}

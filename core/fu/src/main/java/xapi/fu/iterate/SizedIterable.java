package xapi.fu.iterate;

import xapi.fu.Immutable;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.has.HasSize;

import java.util.Iterator;

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
}

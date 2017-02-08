package xapi.fu.iterate;

import xapi.fu.has.HasSize;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/5/17.
 */
public interface SizedIterator<T> extends Iterator<T>, HasSize {
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
        };
    }
}

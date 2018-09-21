package xapi.fu.itr;

import xapi.fu.In1Out1;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/2/18.
 */
public class MultiIterable <T> implements MappedIterable<T> {

    private final Iterable<Iterable<T>> sources;

    public MultiIterable(Iterable<T> ... sources) {
        this(ArrayIterable.iterate(sources));
    }
    public MultiIterable(Iterable<Iterable<T>> sources) {
        this.sources = sources;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            final Iterator<Iterable<T>> inner = sources.iterator();
            Iterator<T> next;
            @Override
            public boolean hasNext() {
                while (next == null || !next.hasNext()) {
                    if (inner.hasNext()) {
                        next = inner.next().iterator();
                    } else {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public T next() {
                boolean valid = hasNext();
                if (!valid) {
                    throw new NoSuchElementException("Corrupted state iterating " + sources);
                }
                return next.next();
            }
        };
    }

    @SafeVarargs
    public static <T> SizedIterable<T> concatFlat(Iterable<Iterable<T>> ... sources) {
        return new MultiIterable<>(sources)
            .flatten(In1Out1.identity())
            .counted();
    }
    @SafeVarargs
    public static <T> MultiIterable<T> concat(Iterable<T> ... sources) {
        return new MultiIterable<>(sources);
    }
}

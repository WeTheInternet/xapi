package xapi.fu.iterate;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
public final class EmptyIterator <I> implements Iterator <I> {

    public static final Iterator EMPTY = new EmptyIterator();
    public static final Iterable NONE = ()->EMPTY;

    public static <I> Iterable<I> none() {
        return NONE;
    }

    public static <I> Iterator<I> empty() {
        return EMPTY;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public I next() {
        throw new UnsupportedOperationException();
    }
}

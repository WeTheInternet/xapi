package xapi.fu.iterate;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
public final class EmptyIterator <I> implements Iterator <I> {

    public static final Iterable NONE = EmptyIterator::new;

    public static <I> Iterable<I> none() {
        return NONE;
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

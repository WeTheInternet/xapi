package xapi.collect.impl;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/4/16.
 */
public final class EmptyIterable <T> implements Iterator <T>, Iterable<T> {

    public static final EmptyIterable EMPTY = new EmptyIterable();

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        throw new IllegalStateException("No items in an empty Iterable");
    }
}

package xapi.fu.iterate;

import xapi.fu.iterate.Chain.ChainBuilder;
import xapi.fu.has.HasSize;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/4/16.
 */
public class CountedIterator <T> implements Iterator<T>, HasSize {

    private final Iterator<T> source;
    private final int size;

    public static <T> CountedIterator<T> count(Iterator<T> itr) {
        return itr instanceof CountedIterator ? (CountedIterator<T>) itr : new CountedIterator<>(itr);
    }

    public static <T> CountedIterator<T> count(T ... itr) {
        return new CountedIterator<>(new ArrayIterable<>(itr));
    }

    public static <T> CountedIterator<T> count(Iterable<T> iterable) {
        final Iterator<T> itr = iterable.iterator();
        return itr instanceof CountedIterator ? (CountedIterator<T>) itr : new CountedIterator<>(iterable);
    }

    public CountedIterator(Iterable<T> source) {
        if (source instanceof HasSize) {
            size = ((HasSize)source).size();
            this.source = source.iterator();
        } else if (source instanceof Collection) {
            this.size = ((Collection)source).size();
            this.source = source.iterator();
        } else {
            final Iterator<T> itr = source.iterator();
            if (itr instanceof HasSize) {
                this.size = ((HasSize)itr).size();
                this.source = itr;
            } else {
                ChainBuilder<T> items = Chain.startChain();
                while (itr.hasNext()) {
                    items.add(itr.next());
                }
                size = items.size();
                this.source = items.iterator();
            }
        }
    }

    public CountedIterator(Iterator<T> source) {
        if (source instanceof HasSize) {
            size = ((HasSize)source).size();
            this.source = source;
        } else {
            ChainBuilder<T> items = Chain.startChain();
            int count = 0;
            while (source.hasNext()) {
                items.add(source.next());
                count++;
            }
            size = count;
            this.source = source;
        }
    }

    @Override
    public boolean hasNext() {
        return source.hasNext();
    }

    @Override
    public T next() {
        return source.next();
    }

    @Override
    public void remove() {
        source.remove();
    }

    @Override
    public int size() {
        return size;
    }
}

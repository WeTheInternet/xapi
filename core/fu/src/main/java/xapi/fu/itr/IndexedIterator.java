package xapi.fu.itr;

import java.util.Iterator;

/**
 * An iterator that counts as it is consumed.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 12/3/17.
 */
public class IndexedIterator <T> implements Iterator<T> {

    private final Iterator<T> src;
    private volatile int ind = -1;

    public IndexedIterator(Iterator<T> src) {
        this.src = src;
    }

    public int getIndex() {
        return ind;
    }

    @Override
    public boolean hasNext() {
        return src.hasNext();
    }

    @Override
    public T next() {
        ind++;
        return src.next();
    }
}

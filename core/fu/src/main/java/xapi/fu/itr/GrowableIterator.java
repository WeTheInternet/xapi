package xapi.fu.itr;

import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.Out1;

import static xapi.fu.itr.SingletonIterator.singleItem;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class GrowableIterator<T> implements Iterator <T> {

    private Lazy<Iterator<T>> mine;
    private GrowableIterator<T> next;
    private final GrowableIterator<T> head;

    @Override
    public boolean hasNext() {
        if (mine.out1().hasNext()) {
            return true;
        } else {
            if (next != null) {
                synchronized (head) {
                    mine = next.mine;
                    next = next.next;
                    assert next != this;
                    return hasNext();
                }
            }
        }
        return false;
    }

    @Override
    public T next() {
        return mine.out1().next();
    }

    public GrowableIterator() {
        this(EmptyIterator.NONE);
    }

    public GrowableIterator(T value) {
        this(SingletonIterator.singleItem(value));
    }

    public GrowableIterator(Iterator<T> mine) {
        this(Lazy.immutable1(mine));
    }

    public GrowableIterator(Iterable<T> mine) {
        this(new Out1<Iterator<T>>() {
            @Override
            public Iterator<T> out1() {
                return mine.iterator();
            }
        });
    }

    public GrowableIterator(Out1<Iterator<T>> mine) {
        this.mine = Lazy.deferred1(mine::out1);
        head = this;
    }

    protected GrowableIterator(Iterator<T> mine, GrowableIterator<T> head) {
        this(Lazy.immutable1(mine), head);
    }

    protected GrowableIterator(Out1<Iterator<T>> mine, GrowableIterator<T> head) {
        this.mine = Lazy.deferred1(mine::out1);
        this.head = head;
    }

    public GrowableIterator<T> concat(T item) {
        return concat(singleItem(item));
    }

    public GrowableIterator<T> concat(Iterator<T> itr) {
        return concatDeferred(Immutable.immutable1(itr));
    }

    public GrowableIterator<T> concat(Iterable<T> itr) {
        return concatDeferred(itr::iterator);
    }

    public GrowableIterator<T> concatDeferred(Out1<Iterator<T>> itr) {

        GrowableIterator<T> node = this;
        while (node.next != null) {
            node = node.next;
        }
        node.next = new GrowableIterator<>(itr, head);
        return this;
    }

    public GrowableIterator<T> concatImmediate(Out1<Iterator<T>> itr) {

        GrowableIterator<T> node = this;
        while (node.next != null) {
            node = node.next;
        }
        node.next = new GrowableIterator<>(itr.out1(), head);
        return this;
    }

    public GrowableIterator<T> insert(T item) {
        return insert(singleItem(item));
    }

    public GrowableIterator<T> insert(Iterator<T> itr) {
        return insertDeferred(Immutable.immutable1(itr));
    }

    public GrowableIterator<T> insert(Iterable<T> itr) {
        return insertDeferred(new Out1<Iterator<T>>() {
            @Override
            public Iterator<T> out1() {
                return itr.iterator();
            }
        });
    }

    public GrowableIterator<T> insertDeferred(Out1<Iterator<T>> itr) {
        synchronized (head) {
            final GrowableIterator<T> myNext = next;
            next = new GrowableIterator<>(itr, head);
            next.next = myNext;
        }
        return this;
    }

    public GrowableIterator<T> insertImmediate(Out1<Iterator<T>> itr) {
        synchronized (head) {
            final GrowableIterator<T> myNext = next;
            next = new GrowableIterator<>(itr.out1(), head);
            next.next = myNext;
        }
        return this;
    }

    public GrowableIterable<T> forAll() {
        return ()->head;
    }

    public GrowableIterable<T> forEachRemaining() {
        return ()->this;
    }

    public static <T> GrowableIterator<T> of(Iterable<T> one) {
        return one instanceof GrowableIterable ?
            ((GrowableIterable<T>) one ).iterator() :
            new GrowableIterator<>(one);
    }

    public static <T> GrowableIterator<T> of(Iterator<T> one) {
        return one instanceof GrowableIterator ?
            (GrowableIterator<T>) one :
            new GrowableIterator<>(one);
    }

    public static <T> GrowableIterator<T> of(T one) {
        return new GrowableIterator<>(SingletonIterator.singleItem(one));
    }

}

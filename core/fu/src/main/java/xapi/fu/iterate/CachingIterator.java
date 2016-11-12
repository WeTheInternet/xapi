package xapi.fu.iterate;

import xapi.fu.*;
import xapi.fu.Filter.Filter1;
import xapi.fu.iterate.Chain.ChainBuilder;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/8/16.
 */
public class CachingIterator <T> implements Iterator<T>, Rethrowable {

    private volatile Out2<Boolean, T> next;
    private volatile Out1<Out2<Boolean, T>> getNext;
    private final Object lock;

    public static <T> CachingIterator<T> cachingIterator(Iterator<T> itr) {
        if (itr instanceof CachingIterator) {
            return (CachingIterator<T>) itr;
        }
        return new CachingIterator<>(itr);
    }

    public CachingIterator(Iterator<T> wrapped) {
        lock = new Object(); // binary semaphore, to prevent concurrent modification
        // To start with, our getNext returns an iterator that accesses the wrapped source object
        // If you peekAll() or filterUntil() on this iterator, this accessor method will be
        // replaced with one that no longer holds the source iterator in scope.
        getNext =
            ()->
                ()-> {
                    final boolean hasNext = wrapped.hasNext();
                    final Out1[] value = new Out1[]{
                        hasNext ? Out1.TRUE : Out1.FALSE,
                        hasNext ? Lazy.deferred1(wrapped::next) : null
                    };
                    next = ()->value;
                    return value;
            };
        advance();
    }

    private void advance() {
        mutex(()-> next = getNext.out1());
    }

    protected T mutex(Out1<T> task) {
       synchronized (lock) {
           return task.out1();
       }
    }
    private void mutex(Do task) {
        mutex(task.returns1(null));
    }

    @Override
    public boolean hasNext() {
        return next.out1();
    }

    public boolean hasNoMore() {
        return !next.out1();
    }

    public T peek() {
        return next.out2();
    }

    public void peekAll(In1<T> callback) {
        peekWhileTrue(callback.filtered(Filter.alwaysTrue()));
    }

    public void peekWhileFalse(Filter1<T> filter) {
        peekWhileTrue(filter.inverse());
    }

    public void peekWhileTrue(Filter1<T> filter) {
        if (hasNext()) {
            mutex(()->{
                ChainBuilder<Out2<Boolean, T>> values = Chain.startChain();
                T cur = next();
                if (!filter.filter1(cur)) {
                    return;
                }
                boolean more = hasNext();
                values.add(Out2.out2Immutable(more, cur));
                while (more) {
                    cur = next();
                    if (!filter.filter1(cur)) {
                        return;
                    }
                    more = hasNext();
                    values.add(Out2.out2Immutable(more, cur));
                }
                final Iterator<Out2<Boolean, T>> itr = values.iterator();
                getNext = itr::next;
                next = getNext.out1();
            });
        }
    }

    @Override
    public T next() {
        try {
            return next.out2();
        } finally {
            advance();
        }
    }
}

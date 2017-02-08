package xapi.fu.iterate;

import xapi.fu.*;
import xapi.fu.Filter.Filter1;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/8/16.
 */
public class CachingIterator <T> implements Iterator<T>, Rethrowable {

    public interface ReplayableIterable<T> extends MappedIterable<T> {}

    private volatile boolean checking;
    private volatile Out2<Boolean, T> next;
    private volatile Out1<Out2<Boolean, T>> getNext;
    private final Object lock;
    private final ChainBuilder<T> vals;

    public static <T> CachingIterator<T> cachingIterator(Iterator<T> itr) {
        if (itr instanceof CachingIterator) {
            return (CachingIterator<T>) itr;
        }
        return new CachingIterator<>(itr);
    }

    public static <T> ReplayableIterable<T> cachingIterable(Iterable<T> itr) {
        if (itr instanceof ReplayableIterable) {
            return (ReplayableIterable<T>) itr;
        } else {
            return cachingIterable(itr.iterator());
        }
    }
    public static <T> ReplayableIterable<T> cachingIterable(Iterator<T> itr) {
        final CachingIterator<T> cacher = cachingIterator(itr);
        return cacher.replayable();
    }

    public void clear() {
        mutex(()->{
            vals.clear();
            next = Out2.out2Immutable(false, null);
            getNext = ()->next;
        });
    }

    public ReplayableIterable<T> replayable() {
        return this::replay;
    }

    @SuppressWarnings("unchecked")
    public Iterator <T> replay() {
        final In1Out1<Out1<?>, Object> mutex = this::mutex;
        return new Iterator<T>() {
            Chain<T> head = vals.build();
            @Override
            public boolean hasNext() {
                return (Boolean)mutex.io(()-> {
                    final boolean wasChecking = checking;
                    try {

                        checking = true;
                        if (head == head.tail.io(null)) {
                            return CachingIterator.this.hasNext();
                        }
                    } finally {
                        checking = wasChecking;
                    }
                    return true;
                });
            }

            @Override
            public T next() {
                return (T)mutex.io(()-> {
                    if (head == head.tail.io(null)) {
                        // If our head is at the tail (no more cached items to view),
                        // then we go ahead and ask the external caching iterator for an item
                        final T val = CachingIterator.this.next();
                        // Be sure to update our pointer to be the tail;
                        // if everyone uses mutex(), this is safe.
                        head = head.tail.io(null);
                        return val;
                    }
                    try {
                        // We have items on our output chain;
                        // because someone else has iterated past this
                        // point before us (in races, mutex() ensures
                        // all elements will be seen).
                        return head.value.out1();
                    } finally {
                        head = head.next;
                    }
                });
            }
        };
    }


    public CachingIterator(Iterator<T> wrapped) {
        lock = new Object();
        // To start with, our getNext returns an iterator that accesses the wrapped source object
        // If you peekAll() or filterUntil() on this iterator, this accessor method will be
        // replaced with one that no longer holds the source iterator in scope.
        vals = Chain.startChain();
        getNext =
            ()->
                ()-> mutex(()->{

                    final boolean hasNext = wrapped.hasNext();
                    final Out1[] value = new Out1[]{
                        hasNext ? Out1.TRUE : Out1.FALSE,
                        null
                    };
                    if (hasNext) {
                        value[1] = Lazy.deferred1(()->vals.addReturnValue(wrapped.next()))
                                .map(i-> {
                                    // Once you have next()'d an element, this node must return false for hasNext.
                                    if (!checking) {
                                        value[0] = Out1.FALSE;
                                    }
                                    return i;
                                });
                    }
                    next = ()->value;
                    return value;
            });
        advance();
    }

    private void advance() {
        mutex(()-> next = getNext.out1());
    }

    protected <R> R mutex(Out1<R> task) {
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
                GrowableIterator<Out2<Boolean, T>> growable = GrowableIterator.of(itr)
                        .concat(new Iterator<Out2<Boolean, T>>() {
                            final Out1<Out2<Boolean, T>> remaining = getNext;
                            Out2<Boolean, T> myNext = next;
                            @Override
                            public boolean hasNext() {
                                return mutex(()->myNext.out1());
                            }

                            @Override
                            public Out2<Boolean, T> next() {
                                return mutex(()->{
                                    try {
                                        return myNext;
                                    } finally {
                                        myNext = remaining.out1();
                                    }
                                });
                            }
                        });
                getNext = growable::next;
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

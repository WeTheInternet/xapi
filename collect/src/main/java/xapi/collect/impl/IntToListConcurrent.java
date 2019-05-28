package xapi.collect.impl;

import xapi.collect.api.CollectionOptions;
import xapi.fu.Do;
import xapi.fu.In2;
import xapi.fu.Out1;
import xapi.fu.has.HasLock;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/31/17.
 */
public class IntToListConcurrent<T> extends IntToList<T> implements HasLock {

    private final ReentrantLock lock = new ReentrantLock();

    public <Generic extends T> IntToListConcurrent(Class<Generic> cls) {
        this(cls, CollectionOptions.asMutableList().concurrent(true).build());
    }

    public <Generic extends T> IntToListConcurrent(Class<Generic> cls, CollectionOptions opts) {
        super(cls, opts);
    }

    public <Generic extends T, L extends List<T>> IntToListConcurrent(
        Class<Generic> cls,
        L list,
        In2<L, Integer> resizer
    ) {
        super(cls, list, resizer);
    }

    @Override
    public <O> O mutex(Out1<O> task) {
        // instead of using synchronized on our object,
        // lets prefer using a private lock,
        // in case calling code decides to use our monitor for something
        // (which would be kind of silly, considering we have the mutex already)
        lock.lock();
        try {
            return task.out1();
        } finally {
            lock.unlock();
        }
    }

}

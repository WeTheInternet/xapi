package xapi.scope.impl;

import xapi.fu.Out1;
import xapi.fu.has.HasLock;
import xapi.fu.has.HasReset;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/17.
 */
public class ThreadLocalDeferred <T> extends ThreadLocal<T> {

    private final Out1<T> supplier;

    public ThreadLocalDeferred(Out1<T> o) {
        this.supplier = o;
    }

    @Override
    protected T initialValue() {
        return supplier.out1();
    }

    public void clear() {
        HasLock.maybeLock(supplier, ()->{
            if (supplier instanceof HasReset) {
                ((HasReset) supplier).reset();
            }
            remove();
            return null;
        });
    }

    /**
     * Final so you override clear() or get().
     *
     * If you need this changed, just send a compelling message.
     *
     * @return whatever happens when the Out1 supplier you gave is invoked again.
     *
     */
    public final T reset() {
        clear();
        return get();
    }
}

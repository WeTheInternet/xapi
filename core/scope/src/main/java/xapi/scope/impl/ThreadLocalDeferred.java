package xapi.scope.impl;

import xapi.fu.Out1;

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

    public T reset() {
        remove();
        return get();
    }
}

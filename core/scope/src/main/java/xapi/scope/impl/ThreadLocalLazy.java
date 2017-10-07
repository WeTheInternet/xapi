package xapi.scope.impl;

import xapi.fu.Lazy;
import xapi.fu.Out1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/17.
 */
public class ThreadLocalLazy<T> extends ThreadLocalDeferred<T> {

    public ThreadLocalLazy(Out1<T> o) {
        super(Lazy.deferred1(o));
    }

}

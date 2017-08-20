package xapi.fu.has;

import xapi.fu.Do;
import xapi.fu.Out1;
import xapi.fu.api.DoNotOverride;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/6/17.
 */
public interface HasLock {

    @DoNotOverride("Override mutex(Out1) instead")
    default void mutex(Do o) {
        mutex(o.returns1(null));
    }

    default <O> O mutex(Out1<O> o) {
        synchronized (getLock()) {
            return o.out1();
        }
    }

    default Object getLock() {
        return this;
    }

}

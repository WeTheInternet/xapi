package xapi.fu.data;

import xapi.fu.In1;

/**
 * A data collection that is "all"able.
 *
 * You send a callback to an all() function,
 * which is invoked immediately with all known items.
 * Then, when a new item is added, your callback is similarly invoked.
 *
 * There is an overload available which also specifies a removal callback.
 * Implementors should check `callback != In1.ignored()` to optimize against no-ops.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 3:01 AM.
 */
public interface Allable<T> {

    default void all(In1<T> callback) {
        all(callback, In1.ignored());
    }

    void all(In1<T> onAdd, In1<T> onRemove);
}

package xapi.fu.data;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.api.Ignore;
import xapi.fu.api.ShouldOverride;

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

    default Do all(In1<T> callback) {
        return all(callback, In1.ignored());
    }

    /**
     * Read all the current values in this collection, but do not listen for changes.
     *
     * You should override this to just directly iterate the real source data and call the callback.
     *
     * @param callback The callback to invoke on each item in this collection.
     * @return this, for chaining
     */
    @ShouldOverride("Iterate source data directly")
    default Allable<T> snapshot(In1<T> callback) {
        all(callback, In1.ignored()).done();
        return this;
    }

    @Ignore("model")
    Do all(In1<T> onAdd, In1<T> onRemove);
}

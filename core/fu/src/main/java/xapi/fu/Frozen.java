package xapi.fu;

import xapi.fu.later.Later;

/**
 * A marker interface we apply to immutable types.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Frozen {

    default boolean isFrozen() {
        return true;
    }

    static boolean isFrozen(Object frozen) {
        if (frozen instanceof Frozen) {
            return true;
        }
        // TODO: consider allowing foreign-classloaded objects that implement Frozen pass
        return false;
    }
}

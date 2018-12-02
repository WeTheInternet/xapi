package xapi.fu.has;

import xapi.fu.api.DoNotOverride;

/**
 * An interface for objects which can communicate a resolution state.
 *
 * This uses a simple boolean; if you need an enum state machine,
 * you should build that yourself, so you don't inherit states you don't need.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/1/18 @ 10:51 PM.
 */
public interface HasResolution {

    boolean isResolved();

    @DoNotOverride
    default boolean isUnresolved() {
        return !isResolved();
    }
}

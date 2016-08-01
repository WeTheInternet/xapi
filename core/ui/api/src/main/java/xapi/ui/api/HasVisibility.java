package xapi.ui.api;

import xapi.fu.Pointer;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public interface HasVisibility {

    Pointer<Boolean> visibility();

    default boolean isVisible() {
        return !Boolean.FALSE.equals(visibility().out1());
    }

    default void setVisible(boolean visible) {
        visibility().in(visible);
    }

}

package xapi.ui.api.components;

import xapi.ui.api.UiElement;

/**
 * Created by james on 6/7/16.
 */
public interface Input <T> extends UiElement {

    T getValue();

    void setValue(T value);

}

package xapi.ui.api.components;

import xapi.ui.api.UiElement;

/**
 * Created by james on 6/7/16.
 */
public interface Window <RootType, Type extends RootType, Self extends Window<RootType, Type, Self>> extends UiElement <RootType, Self> {
}

package xapi.ui.api.components;

import xapi.ui.api.UiElement;

/**
 * Created by james on 6/7/16.
 */
public interface Window <Node, Element extends Node, Self extends UiElement<Node, ? extends Node, Self>>
    extends UiElement <Node, Element, Self> {
}

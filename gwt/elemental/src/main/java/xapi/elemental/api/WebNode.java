package xapi.elemental.api;

import elemental.dom.Element;
import elemental.dom.Node;
import xapi.ui.api.UiNode;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/5/17.
 */
public interface WebNode <E extends Element, Base extends UiNode<Node, ? extends Node, Base>> extends UiNode<Node, E, Base> {
}

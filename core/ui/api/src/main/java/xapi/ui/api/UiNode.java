package xapi.ui.api;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import xapi.fu.api.Ignore;
import xapi.fu.iterate.SizedIterable;

/**
 * An arbitrary ui node which wraps some kind of "native element".
 *
 * We may start to pull methods from {@link UiElement} up to this interface,
 * but we are purposely keeping this type limited to the structure of a ui node,
 * so you can insert non-ui layers without requiring dependency on native types.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/5/17.
 */
@JsType
public interface UiNode <Node, Element extends Node, BaseType extends UiNode<Node, ? extends Node, BaseType>> {

    BaseType getParent();

    Element getElement();

    @JsIgnore
    @Ignore
    SizedIterable<BaseType> getChildren();

}

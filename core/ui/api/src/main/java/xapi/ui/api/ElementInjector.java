package xapi.ui.api;

import jsinterop.annotations.JsType;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
@JsType(isNative = true)
public interface ElementInjector <Node, Base extends UiElement<Node, ? extends Node, Base>> {

  void appendChild(Base newChild);

  void insertBefore(Base newChild);

  void insertAtBegin(Base newChild);

  void insertAfter(Base newChild);

  void insertAtEnd(Base newChild);

  void removeChild(Base child);
}

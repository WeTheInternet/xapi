package xapi.elemental.api;

import xapi.ui.api.NodeBuilder;
import elemental.dom.Node;

public class ElementBuilder <E extends Node> {

  NodeBuilder<E> head;

  public E getElement() {
    return head.getElement();
  }

  public ElementBuilder<E> _id(String id) {
    setAttribute("id", id);
    return this;
  }

  private void setAttribute(String key, String value) {

  }

}

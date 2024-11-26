package xapi.components.impl;

import elemental2.dom.Element;
import elemental2.dom.Node;
import jsinterop.base.Js;
import jsinterop.base.JsArrayLike;
import xapi.fu.itr.MappedIterable;

import java.util.Iterator;

public class ElIterable implements MappedIterable<Element> {

  private final JsArrayLike<Node> nodes;
  private final class Itr implements Iterator<Element> {

    int pos = 0;
    private Element was;

    @Override
    public boolean hasNext() {
      if (was != null && nodes.getAt(pos-1) != was) {
        // In case somebody is removing node while we are iterating,
        // we will rewind
        pos --;
      }
      was = null;
      while(pos < nodes.getLength()) {
        if (nodes.getAt(pos).nodeType == Node.ELEMENT_NODE)
          return true;
        pos++;
      }
      return false;
    }

    @Override
    public Element next() {
      was = Js.cast(nodes.getAt(pos++));
      return was;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  private ElIterable(JsArrayLike<Node> nodes) {
    this.nodes = nodes;
  }

  @Override
  public Iterator<Element> iterator() {
    return new Itr();
  }

  public static MappedIterable<Element> forEach(JsArrayLike<Node> children) {
    return new ElIterable(children);
  }

  public static MappedIterable<Element> forEach(Node node) {
    return new ElIterable(node.childNodes);
  }

}

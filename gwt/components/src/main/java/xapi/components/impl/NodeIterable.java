package xapi.components.impl;

import elemental2.dom.HTMLCollection;
import elemental2.dom.Node;
import elemental2.dom.NodeList;
import jsinterop.base.Js;
import jsinterop.base.JsArrayLike;
import xapi.fu.MappedIterable;

import java.util.Iterator;

public class NodeIterable implements MappedIterable<Node> {

  private final JsArrayLike<Node> nodes;
  private final class Itr implements Iterator<Node> {

    int pos = 0;
    private Node was;

    @Override
    public boolean hasNext() {
      if (was != null && nodes.getAt(pos-1) != was) {
        // In case somebody is removing node while we are iterating,
        // we will rewind
        pos --;
      }
      was = null;
      if (pos < nodes.getLength()) {
          return true;
      }
      return false;
    }

    @Override
    public Node next() {
      was = Js.uncheckedCast(nodes.getAt(pos++));
      return was;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  private NodeIterable(HTMLCollection nodes) {
    this.nodes = nodes;
  }

  private NodeIterable(NodeList nodes) {
    this.nodes = nodes;
  }

  @Override
  public Iterator<Node> iterator() {
    return new Itr();
  }

  public static MappedIterable<Node> forEach(HTMLCollection children) {
    return new NodeIterable(children);
  }

  public static MappedIterable<Node> forEach(Node node) {
    return new NodeIterable(node.childNodes);
  }

  public static MappedIterable<Node> forEach(NodeList children) {
    return new NodeIterable(children);
  }

}

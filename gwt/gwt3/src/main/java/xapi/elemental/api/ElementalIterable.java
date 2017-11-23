package xapi.elemental.api;

import elemental2.dom.Element;
import elemental2.dom.HTMLCollection;
import elemental2.dom.Node;
import elemental2.dom.NodeList;
import jsinterop.base.JsArrayLike;
import xapi.fu.MappedIterable;

import java.util.Iterator;

public class ElementalIterable implements MappedIterable<Element> {

  private final JsArrayLike<Element> nodes;
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
      while(pos < nodes.getLength()) {
        if (((Node)nodes.getAt(pos)).nodeType == Node.ELEMENT_NODE)
          return true;
        pos++;
      }
      was = null;
      return false;
    }

    @Override
    public Element next() {
      was = nodes.getAt(pos++);
      return was;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  private ElementalIterable(HTMLCollection nodes) {
    this.nodes = nodes;
  }

  private ElementalIterable(NodeList nodes) {
    this.nodes = nodes;
  }

  @Override
  public Iterator<Element> iterator() {
    return new Itr();
  }

  public static MappedIterable<Element> forEach(HTMLCollection<Element> children) {
    return new ElementalIterable(children);
  }

  public static MappedIterable<Element> forEach(Node node) {
    return new ElementalIterable(node.childNodes);
  }

  public static MappedIterable<Element> forEach(NodeList children) {
    return new ElementalIterable(children);
  }

}

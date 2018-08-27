package xapi.elemental.api;

import elemental2.dom.Element;
import elemental2.dom.HTMLCollection;
import elemental2.dom.Node;
import elemental2.dom.NodeList;
import jsinterop.base.JsArrayLike;
import xapi.fu.iterate.SizedIterable;
import xapi.fu.iterate.SizedIterator;

public class ElementalIterable implements SizedIterable<Element> {

  private final JsArrayLike<Element> nodes;
  private final class Itr implements SizedIterator<Element> {

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

    @Override
    public int size() {
      return nodes.getLength() - pos;
    }
  }

  private ElementalIterable(HTMLCollection nodes) {
    this.nodes = nodes;
  }

  private ElementalIterable(NodeList nodes) {
    this.nodes = nodes;
  }

  @Override
  public SizedIterator<Element> iterator() {
    return new Itr();
  }

  public static SizedIterable<Element> forEach(HTMLCollection<Element> children) {
    return new ElementalIterable(children);
  }

  public static SizedIterable<Element> forEach(Node node) {
    return new ElementalIterable(node.childNodes);
  }

  public static SizedIterable<Element> forEach(NodeList children) {
    return new ElementalIterable(children);
  }

  @Override
  public int size() {
    return nodes.getLength();
  }
}

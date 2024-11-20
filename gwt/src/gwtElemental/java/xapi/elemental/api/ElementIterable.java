package xapi.elemental.api;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.NodeList;
import elemental.html.HTMLCollection;
import elemental.util.Indexable;
import xapi.fu.itr.MappedIterable;

import java.util.Iterator;

public class ElementIterable implements MappedIterable<Element> {

  private final Indexable nodes;
  private final class Itr implements Iterator<Element> {

    int pos = 0;
    private Element was;

    @Override
    public boolean hasNext() {
      if (was != null && nodes.at(pos-1) != was) {
        // In case somebody is removing node while we are iterating,
        // we will rewind
        pos --;
      }
      while(pos < nodes.length()) {
        if (((Node)nodes.at(pos)).getNodeType() == Node.ELEMENT_NODE)
          return true;
        pos++;
      }
      was = null;
      return false;
    }

    @Override
    public Element next() {
      was = (Element) nodes.at(pos++);
      return was;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  private ElementIterable(HTMLCollection nodes) {
    this.nodes = nodes;
  }

  private ElementIterable(NodeList nodes) {
    this.nodes = nodes;
  }

  @Override
  public Iterator<Element> iterator() {
    return new Itr();
  }

  public static MappedIterable<Element> forEach(HTMLCollection children) {
    return new ElementIterable(children);
  }

  public static MappedIterable<Element> forEach(Node node) {
    return new ElementIterable(node.getChildNodes());
  }

  public static MappedIterable<Element> forEach(NodeList children) {
    return new ElementIterable(children);
  }

}

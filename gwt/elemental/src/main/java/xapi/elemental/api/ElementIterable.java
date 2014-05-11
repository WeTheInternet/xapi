package xapi.elemental.api;

import java.util.Iterator;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental.dom.NodeList;
import elemental.html.HTMLCollection;
import elemental.util.Indexable;

public class ElementIterable implements Iterable<Element> {

  private final Indexable nodes;
  private final class Itr implements Iterator<Element> {

    int pos = 0;

    @Override
    public boolean hasNext() {
      while(pos < nodes.length()) {
        if (((Node)nodes.at(pos)).getNodeType() == Node.ELEMENT_NODE)
          return true;
        pos++;
      }
      return false;
    }

    @Override
    public Element next() {
      return (Element)nodes.at(pos++);
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

  public static Iterable<Element> forEach(HTMLCollection children) {
    return new ElementIterable(children);
  }

  public static Iterable<Element> forEach(NodeList children) {
    return new ElementIterable(children);
  }

}

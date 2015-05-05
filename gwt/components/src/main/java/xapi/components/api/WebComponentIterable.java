/**
 *
 */
package xapi.components.api;

import java.util.Iterator;

import elemental.dom.Element;

/**
 * This class is designed to transform an Iterable of {@link Element} into an Iterable of a WebComponent type.
 * It is, in essence, simply performing an unsafe cast for you.
 * <p>
 * Recommend usage is to use element.querySelector("my-web-component-name") to get a NodeList of elements,
 * transformed into an Iterable<Element> via ElementIterable in the xapi-elemental module.  From there,
 * you can use this wrapper to transform your elements into web components.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class WebComponentIterable <W extends IsWebComponent<?>> implements Iterable<W>{

  public static <W extends IsWebComponent<?>> Iterable<W> asWebComponents(final Iterable<Element> iter) {
    return new WebComponentIterable<W>(iter);
  }

  private final class Itr implements Iterator<W> {

    private final Iterator<Element> itr;

    public Itr(final Iterator<Element> iterator) {
      this.itr = iterator;
    }

    @Override
    public boolean hasNext() {
      return itr.hasNext();
    }

    @Override
    @SuppressWarnings("unchecked")
    public W next() {
      return (W)itr.next();
    }

  }

  private final Iterable<Element> iter;

  public WebComponentIterable(final Iterable<Element> source) {
    this.iter = source;
  }

  @Override
  public Iterator<W> iterator() {
    return new Itr(iter.iterator());
  }

}

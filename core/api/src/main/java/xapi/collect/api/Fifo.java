package xapi.collect.api;

import xapi.collect.impl.SimpleFifo;
import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.Out2;

import java.util.Iterator;


/**
 * A simple fifo interface, for lightweight collections support.
 * It's a simply First in, first out linked queue.
 *
 * For the sake of trim code, and aiding in gwt compiler optimization,
 * our core library should not use java.util, nor other collections framework.
 *
 * This is so gwt code doesn't take a codesize hit for using the tool.
 * Jre-only code is free to use any framework.
 *
 * The default implementaion, {@link SimpleFifo} is threadsafe;
 * explicitly single-threaded environments may want to override this
 * in places where a Fifo is injected (core libraries just use new {@link SimpleFifo}).
 * GWT just uses an overlay on a native [] to implement fifo.
 *
 * This collection will only throw exceptions if you are doing something illegal,
 * like using Iterator.remove() before calling .next().
 *
 * null adds are silently ignored,
 * and take() will only return null if the fifo is drained.
 *
 * This is so subclasses can override isEmpty() to return false until a
 * given resource is closed, or a set of Future<>s complete.
 *
 * take() can return null while isEmpty return false.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <E>
 */
public interface Fifo <E> {

  /**
   * Analagous to add(), however, we do not use the standard naming convention,
   * to avoid interface clashes with adapter types in collection libraries
   * that may wish to override our Fifo with their own type.
   *
   * @param item - The item to add to end of queue.
   * @return this queue
   */
  Fifo <E> give(E item);

  /**
   * Analagous to addAll(), however, we avoid the standard naming convention,
   * so we can provide a fluent, strongly typed api.
   *
   * Also, since the javascript object, JsFifo, is an array itself,
   * the "cost" of the varargs is diminished because we can just 'cat them together.
   *
   * @param elements - Varags or array adapters for bulk add.
   * @return - this
   */
  @SuppressWarnings("unchecked")
  Fifo <E> giveAll(E ... elements);

  /**
   * Analagous to addAll(), however, we avoid the standard naming convention,
   * so we can provide a fluent, strongly typed api.
   *
   * @param elements - Any collection or custom iterable adapters (like blocking / async).
   * @return - this
   */
  Fifo <E> giveAll(Iterable<E> elements);

  /**
   * Analagous to poll(); retrieves and removes head.
   *
   * We avoid the use of standard queue naming methods,
   * in case an api to be injected uses Object instead of &lt;Generic&gt;
   *
   * @return and remove the head of the queue
   */
  E take();

  /**
   * Fastest way to tell if the queue is drained.
   *
   * @return true if head == tail (there are no elements)
   */
  boolean isEmpty();

  /**
   * Convenience method for !isEmpty()
   */
  default boolean isNotEmpty() {
    return !isEmpty();
  }
  /**
   * Check if this queue contains the given item.  O(n) performance.
   *
   * You may want to use a Set unless you know there aren't many items.
   *
   * @param item
   * @return
   */
  boolean contains(E item);

  /**
   * Manually remove the item from queue.  O(n) performance.
   * @param item
   * @return
   */
  boolean remove(E item);
  /**
   * Return a count of items in the queue.
   *
   * Default implementation uses a counter; subclasses may have O(n) if
   * they must transverse the nodes to get a count.
   * @return
   */
  int size();
  /**
   * Removes all items in the queue.
   * head = tail;
   */
  void clear();
  /**
   * @return An iterator for the items in the queue.
   *
   * We do NOT implement Iterable, otherwise GWT JSOs will have a fit.
   */
  Iterator<E> iterator();

  /**
   * @return an Iterable for these items; JRE runtimes will likely return this;
   * GWT runtimes will return an iterable object.
   *
   */
  Iterable<E> forEach();

  default Fifo<E> out(In1<E> consumer) {
    forEach().forEach(consumer::in);
    return this;
  }

  default <To> void transform(In1Out1<E, To> transform, In1<To> into) {
    out(transform.adapt(into));
  }

  default Out2<Boolean, E> supplier() {
    final Iterator<E> itr = iterator();
    return Out2.out2(itr::hasNext, itr::next);
  }

  String join(String delim);
}

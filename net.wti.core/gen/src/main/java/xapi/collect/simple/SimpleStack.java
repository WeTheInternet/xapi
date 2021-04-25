package xapi.collect.simple;

/**
 * A very simple, but useful stack (linked list).
 * <p>
 * It's one-way, threadsafe, fast, toString friendly, and can merge with other
 * SimpleStacks easily via {@link #consume(AbstractLinkedList)}
 * <p>
 * Note that neither remove() nor size() are not supported.
 * <p>
 * If you need a list or a map, use one. This class is for pushing together
 * references, iterating through them, and maybe joining them into a string.
 *
 * @author james@wetheinter.net
 *
 */
public class SimpleStack<T> extends
  AbstractLinkedList<T, SimpleStack.StackNode<T>, SimpleStack<T>> {

  static class StackNode<T> extends AbstractLinkedList.Node<T, StackNode<T>> {
  }

  @Override
  protected StackNode<T> newNode(final T item) {
    return new StackNode<T>();
  }
}

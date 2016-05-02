package xapi.fu;

import java.util.Iterator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public class MappableIterator <F, T> implements Iterator<T> {
  private final Iterator<F> from;
  private final In1Out1<F, T> mapper;

  public MappableIterator(Iterator<F> from, In1Out1<F, T> mapper) {
    this.from = from;
    this.mapper = mapper;
  }

  @Override
  public boolean hasNext() {
    return from.hasNext();
  }

  @Override
  public T next() {
    final F next = from.next();
    return mapper.io(next);
  }

  @Override
  public void remove() {
    from.remove();
  }
}

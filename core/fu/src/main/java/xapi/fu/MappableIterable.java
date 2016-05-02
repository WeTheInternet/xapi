package xapi.fu;

import java.util.Iterator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public interface MappableIterable <T> extends Iterable <T> {

  static <T> MappableIterable <T> mappable(Iterable<T> itr) {
    return itr::iterator;
  }

  default <F> Iterable<F> map(In1Out1<T, F> mapper) {
    return ()->{
      final Iterator<T> itr = iterator();
      return new MappableIterator<>(itr, mapper);
    };
  }
}

package xapi.source.write;

import xapi.fu.Out1;

/**
 * A simple interface for converting objects into strings.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface ToStringer {

  ToStringer DEFAULT_TO_STRINGER = new DefaultToStringer();

  default String toString(Object o) {
    return String.valueOf(
        o instanceof Out1 ? ((Out1)o).out1() : o
    );
  }

}
final class DefaultToStringer implements ToStringer {

}

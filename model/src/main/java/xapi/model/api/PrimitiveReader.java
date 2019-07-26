/**
 *
 */
package xapi.model.api;

import xapi.source.api.CharIterator;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface PrimitiveReader {

  Object readPrimitive(Class<?> cls, CharIterator src, PrimitiveSerializer primitives);

}

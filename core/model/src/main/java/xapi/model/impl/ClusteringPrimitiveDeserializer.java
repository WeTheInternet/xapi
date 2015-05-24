/**
 *
 */
package xapi.model.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.model.api.PrimitiveSerializer;
import xapi.source.api.CharIterator;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ClusteringPrimitiveDeserializer extends DelegatingPrimitiveSerializer {

  private final IntTo<String> values;

  public ClusteringPrimitiveDeserializer(final PrimitiveSerializer primitives, final CharIterator in) {
    super(primitives);
    values = X_Collect.newList(String.class);
    int size = primitives.deserializeInt(in);
    while (size --> 0) {
      values.add(primitives.deserializeString(in));
    }
  }

  @Override
  public String serializeClass(final Class<?> c) {
    // This class should only be used for deserializing.
    throw new UnsupportedOperationException();
  }

  @Override
  public String serializeString(final String s) {
    // This class should only be used for deserializing.
    throw new UnsupportedOperationException();
  }

  @Override
  public String deserializeString(final CharIterator s) {
    final int pos = deserializeInt(s);
    if (pos == -1) {
      return null;
    }
    return values.at(pos);
  }

  @Override
  public <C> Class<C> deserializeClass(final CharIterator c) {
    final int pos = deserializeInt(c);
    if (pos == -1) {
      return null;
    }
    final String value = values.at(pos);
    return loadClass(value);
  }

}

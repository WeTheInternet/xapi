/**
 *
 */
package xapi.model.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.CharBuffer;
import xapi.model.api.PrimitiveSerializer;
import xapi.source.api.CharIterator;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ClusteringPrimitiveSerializer extends DelegatingPrimitiveSerializer {

  private final CharBuffer out;
  private final StringTo<Integer> classes;
  private final CharBuffer size;

  public ClusteringPrimitiveSerializer(final PrimitiveSerializer primitives, final CharBuffer out) {
    super(primitives);
    this.size = new CharBuffer();
    this.out = new CharBuffer();
    out.addToEnd(this.size);
    out.addToEnd(this.out);
    classes = X_Collect.newStringMap(Integer.class);
  }

  @Override
  public <C> Class<C> deserializeClass(final CharIterator c) {
    // This class should only be used for serializing.
    throw new UnsupportedOperationException();
  }

  @Override
  public String deserializeString(final CharIterator s) {
    // This class should only be used for serializing.
    throw new UnsupportedOperationException();
  }

  @Override
  public String serializeString(final String s) {
    Integer position = classes.get(s);
    if (position == null) {
      position = classes.size();
      classes.put(s, position);
      out.append(super.serializeString(s));
      size.clear();
      size.append(serializeInt(position+1));
    }
    return super.serializeInt(position);
  }
  @Override
  public String serializeClass(final Class<?> c) {
    return serializeString(c.getName());
  }

}

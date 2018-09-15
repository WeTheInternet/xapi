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
  private final StringTo<Integer> strings;
  private final CharBuffer size;

  public ClusteringPrimitiveSerializer(final PrimitiveSerializer primitives, final CharBuffer out) {
    super(primitives);
    this.size = new CharBuffer();
    this.out = new CharBuffer();
    // our final result will contain the total size of pooled strings
    out.addToEnd(this.size);
    // then is followed by the structural content of the serialized message.
    out.addToEnd(this.out);
    strings = X_Collect.newStringMap(Integer.class, X_Collect.MUTABLE_CONCURRENT);
    // TODO: in order to reduce data on the wire, we should have a set of hardcoded "well known types"
    // with negative int ids that are never included in the string pool.  We should also support
    // "connection clustering", where a client connection can send ACK messages for all types added to pool,
    // so we can prime our string map with "strings already known and retained by client".
    // (clients can also send DEL messages when they want to forget some LRU value)
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
    // Null handling remains the same
    if (s == null) {
      return super.serializeInt(-1);
    }
    Integer position = strings.get(s);
    if (position == null) {
      position = strings.size();
      strings.put(s, position);
      out.append(super.serializeString(s));
      size.clear();
      size.append(serializeInt(position+1));
    }
    return super.serializeInt(position);
  }
  @Override
  public String serializeClass(final Class<?> c) {
    // Null handling remains the same
    if (c == null) {
      return super.serializeInt(-1);
    }
    return serializeString(c.getName());
  }

}

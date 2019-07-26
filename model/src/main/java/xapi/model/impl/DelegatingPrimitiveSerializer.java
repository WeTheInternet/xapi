/**
 *
 */
package xapi.model.impl;

import xapi.model.api.PrimitiveSerializer;
import xapi.source.api.CharIterator;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class DelegatingPrimitiveSerializer implements PrimitiveSerializer {

  protected final PrimitiveSerializer primitives;

  public DelegatingPrimitiveSerializer(final PrimitiveSerializer primitives) {
    this.primitives = primitives;
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeBoolean(boolean)
   */
  @Override
  public String serializeBoolean(final boolean z) {
    return primitives.serializeBoolean(z);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeBooleanArray(boolean[])
   */
  @Override
  public String serializeBooleanArray(final boolean... z) {
    return primitives.serializeBooleanArray(z);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeByte(byte)
   */
  @Override
  public String serializeByte(final byte b) {
    return primitives.serializeByte(b);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeShort(short)
   */
  @Override
  public String serializeShort(final short s) {
    return primitives.serializeShort(s);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeChar(char)
   */
  @Override
  public String serializeChar(final char c) {
    return primitives.serializeChar(c);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeInt(int)
   */
  @Override
  public String serializeInt(final int i) {
    return primitives.serializeInt(i);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeLong(long)
   */
  @Override
  public String serializeLong(final long l) {
    return primitives.serializeLong(l);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeFloat(float)
   */
  @Override
  public String serializeFloat(final float f) {
    return primitives.serializeFloat(f);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeDouble(double)
   */
  @Override
  public String serializeDouble(final double d) {
    return primitives.serializeDouble(d);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeString(java.lang.String)
   */
  @Override
  public String serializeString(final String s) {
    return primitives.serializeString(s);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#serializeClass(java.lang.Class)
   */
  @Override
  public String serializeClass(final Class<?> c) {
    return primitives.serializeClass(c);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeBoolean(xapi.source.api.CharIterator)
   */
  @Override
  public boolean deserializeBoolean(final CharIterator z) {
    return primitives.deserializeBoolean(z);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeBooleanArray(xapi.source.api.CharIterator)
   */
  @Override
  public boolean[] deserializeBooleanArray(final CharIterator z) {
    return primitives.deserializeBooleanArray(z);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeByte(xapi.source.api.CharIterator)
   */
  @Override
  public byte deserializeByte(final CharIterator b) {
    return primitives.deserializeByte(b);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeShort(xapi.source.api.CharIterator)
   */
  @Override
  public short deserializeShort(final CharIterator s) {
    return primitives.deserializeShort(s);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeChar(xapi.source.api.CharIterator)
   */
  @Override
  public char deserializeChar(final CharIterator c) {
    return primitives.deserializeChar(c);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeInt(xapi.source.api.CharIterator)
   */
  @Override
  public int deserializeInt(final CharIterator i) {
    return primitives.deserializeInt(i);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeLong(xapi.source.api.CharIterator)
   */
  @Override
  public long deserializeLong(final CharIterator l) {
    return primitives.deserializeLong(l);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeFloat(xapi.source.api.CharIterator)
   */
  @Override
  public float deserializeFloat(final CharIterator f) {
    return primitives.deserializeFloat(f);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeDouble(xapi.source.api.CharIterator)
   */
  @Override
  public double deserializeDouble(final CharIterator d) {
    return primitives.deserializeDouble(d);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeString(xapi.source.api.CharIterator)
   */
  @Override
  public String deserializeString(final CharIterator s) {
    return primitives.deserializeString(s);
  }

  /**
   * @see xapi.model.api.PrimitiveSerializer#deserializeClass(xapi.source.api.CharIterator)
   */
  @Override
  public <C> Class<C> deserializeClass(final CharIterator c) {
    return primitives.deserializeClass(c);
  }

  @Override
  public <C> Class<C> loadClass(final String cls) {
    return primitives.loadClass(cls);
  }

}

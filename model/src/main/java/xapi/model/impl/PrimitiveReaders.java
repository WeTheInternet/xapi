/**
 *
 */
package xapi.model.impl;

import xapi.model.api.PrimitiveReader;
import xapi.model.api.PrimitiveSerializer;
import xapi.source.api.CharIterator;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public final class PrimitiveReaders {

  private PrimitiveReaders() {}

  public static PrimitiveReader forBoolean() {
    return new PrimitiveReader() {
      @Override
      public Object readPrimitive(final Class<?> cls, final CharIterator src, final PrimitiveSerializer primitives) {
        return primitives.deserializeBoolean(src);
      }
    };
  }

  public static PrimitiveReader forByte() {
    return new PrimitiveReader() {
      @Override
      public Object readPrimitive(final Class<?> cls, final CharIterator src, final PrimitiveSerializer primitives) {
        return primitives.deserializeByte(src);
      }
    };
  }

  public static PrimitiveReader forShort() {
    return new PrimitiveReader() {
      @Override
      public Object readPrimitive(final Class<?> cls, final CharIterator src, final PrimitiveSerializer primitives) {
        return primitives.deserializeShort(src);
      }
    };
  }

  public static PrimitiveReader forChar() {
    return new PrimitiveReader() {
      @Override
      public Object readPrimitive(final Class<?> cls, final CharIterator src, final PrimitiveSerializer primitives) {
        return primitives.deserializeChar(src);
      }
    };
  }

  public static PrimitiveReader forInt() {
    return new PrimitiveReader() {
      @Override
      public Object readPrimitive(final Class<?> cls, final CharIterator src, final PrimitiveSerializer primitives) {
        return primitives.deserializeInt(src);
      }
    };
  }

  public static PrimitiveReader forLong() {
    return new PrimitiveReader() {
      @Override
      public Object readPrimitive(final Class<?> cls, final CharIterator src, final PrimitiveSerializer primitives) {
        return primitives.deserializeLong(src);
      }
    };
  }

  public static PrimitiveReader forFloat() {
    return new PrimitiveReader() {
      @Override
      public Object readPrimitive(final Class<?> cls, final CharIterator src, final PrimitiveSerializer primitives) {
        return primitives.deserializeFloat(src);
      }
    };
  }

  public static PrimitiveReader forDouble() {
    return new PrimitiveReader() {
      @Override
      public Object readPrimitive(final Class<?> cls, final CharIterator src, final PrimitiveSerializer primitives) {
        return primitives.deserializeDouble(src);
      }
    };
  }
}

/**
 *
 */
package xapi.model.api;

import xapi.source.lex.CharIterator;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface PrimitiveSerializer {

  String serializeBoolean(boolean z);
  String serializeBooleanArray(boolean ... z);
  String serializeByte(byte b);
  String serializeShort(short s);
  String serializeChar(char c);
  String serializeInt(int i);
  String serializeLong(long l);
  String serializeFloat(float f);
  String serializeDouble(double d);
  String serializeString(String s);
  String serializeClass(Class<?> c);

  boolean deserializeBoolean(CharIterator z);
  boolean[] deserializeBooleanArray(CharIterator z);
  byte deserializeByte(CharIterator b);
  short deserializeShort(CharIterator s);
  char deserializeChar(CharIterator c);
  int deserializeInt(CharIterator i);
  long deserializeLong(CharIterator l);
  float deserializeFloat(CharIterator f);
  double deserializeDouble(CharIterator d);
  String deserializeString(CharIterator s);
  <C> Class<C> deserializeClass(CharIterator c);

  <C> Class<C> loadClass(String cls);

}

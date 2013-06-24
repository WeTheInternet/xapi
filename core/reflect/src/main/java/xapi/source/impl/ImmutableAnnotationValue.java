package xapi.source.impl;

import xapi.source.X_Modifier;
import xapi.source.api.IsAnnotationValue;
import xapi.util.api.ConvertsValue;
import xapi.util.converters.ConvertsStringValue;

public class ImmutableAnnotationValue implements IsAnnotationValue{

  private final Object value;
  private final String type;
  private final int modifier;
  private final ConvertsValue<Object, String> toString;

  public ImmutableAnnotationValue(String type, Object value, int modifier) {
    this(type, value, new ConvertsStringValue<Object>(),  modifier);
  }
  public ImmutableAnnotationValue(String type, Object value, ConvertsValue<Object, String> toString, int modifier) {
    this.type = type;
    this.value = value;
    this.toString = toString;
    this.modifier = modifier;
  }

  @Override
  public boolean isArray() {
    return type.contains("[]");
  }

  @Override
  public boolean isEnum() {
    return modifier == X_Modifier.ENUM;
  }

  @Override
  public boolean isClass() {
    return type.contains("java.lang.Class");
  }

  @Override
  public boolean isAnnotation() {
    return modifier == X_Modifier.ANNOTATION;
  }

  @Override
  public boolean isString() {
    return type.contains("java.lang.String");
  }

  @Override
  public boolean isPrimitive() {
    return modifier == -1;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public Object getRawValue() {
    return value;
  }
  @Override
  public Object loadValue(ClassLoader loader) {
    if (isArray()) {
      // 
    }
    throw new UnsupportedOperationException("loadValue not yet supported");
  }
  
  @Override
  public String toString() {
    return toString.convert(value);
  }
  
}

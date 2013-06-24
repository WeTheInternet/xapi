package xapi.source.api;

public interface IsAnnotationValue {

  boolean isArray();
  boolean isEnum();
  boolean isClass();
  boolean isAnnotation();
  boolean isString();
  boolean isPrimitive();
  String getType();
  Object getRawValue();
  Object loadValue(ClassLoader loader);
  
  
}

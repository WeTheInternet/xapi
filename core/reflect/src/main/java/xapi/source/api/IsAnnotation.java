package xapi.source.api;


public interface IsAnnotation
extends IsType,
HasMethods
{

  IsAnnotationValue getDefaultValue(IsMethod method);

  IsAnnotationValue getValue(IsMethod value);
  
  Object toAnnotation(ClassLoader loader);

  boolean isRuntime();
  boolean isCompile();
  boolean isSource();


}

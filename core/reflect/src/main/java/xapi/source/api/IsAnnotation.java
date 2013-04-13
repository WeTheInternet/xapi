package xapi.source.api;


public interface IsAnnotation
extends IsType,
HasMethods
{

  Object getDefaultValue(IsMethod method);

  Object toAnnotation(ClassLoader loader);

}

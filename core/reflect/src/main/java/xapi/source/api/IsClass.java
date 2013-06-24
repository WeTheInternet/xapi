package xapi.source.api;

public interface IsClass
extends IsMember,
HasMethods,
HasFields,
HasGenerics,
HasInterfaces
{

  boolean isAbstract();
  boolean isFinal();
  boolean isStatic();
  boolean isInterface();
  boolean isAnnotation();
  boolean isEnum();
  IsMethod getEnclosingMethod();
  Iterable<IsClass> getInnerClasses();

  Class<?> toClass(ClassLoader loader) throws ClassNotFoundException;

}

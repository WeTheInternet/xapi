package xapi.source.api;

public interface IsMethod
extends
IsMember,
IsNamedType,
HasEnclosingClass
{

  boolean isAbstract();
  boolean isStatic();
  IsType getReturnType();
  IsType[] getParameters();
  IsGeneric[] getGenerics();
  IsType[] getExceptions();
  IsClass getEnclosingType();
  IsAnnotationValue getDefaultValue();

}

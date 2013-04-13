package xapi.source.api;

public interface IsMethod
extends
IsMember,
HasEnclosingClass
{

  boolean isAbstract();
  boolean isStatic();
  String getName();
  IsType getReturnType();
  IsType[] getParameters();
  IsGeneric[] getGenerics();
  IsType[] getExceptions();
  IsClass getEnclosingType();

}

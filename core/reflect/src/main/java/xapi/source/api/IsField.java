package xapi.source.api;

public interface IsField
extends IsNamedType,
IsMember,
HasEnclosingClass
{

  boolean isStatic();
  boolean isVolatile();
  boolean isTransient();

}

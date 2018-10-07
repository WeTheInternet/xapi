package xapi.source.api;

import xapi.fu.has.HasName;

public interface IsField
extends IsNamedType, // This should be composed, not inherited...
IsMember,
HasEnclosingClass,
HasName
{

  boolean isStatic();
  boolean isVolatile();
  boolean isTransient();

}

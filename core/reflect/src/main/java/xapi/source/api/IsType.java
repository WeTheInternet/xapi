package xapi.source.api;

public interface IsType
extends HasQualifiedName
{

  boolean isPrimitive();
  IsType getEnclosingType();

}

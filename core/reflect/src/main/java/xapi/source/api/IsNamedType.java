package xapi.source.api;

public interface IsNamedType extends IsType{

  String getName();

  @Override
  default boolean isPrimitive() {
    return false;
  }
}

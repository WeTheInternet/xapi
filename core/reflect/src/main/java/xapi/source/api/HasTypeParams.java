package xapi.source.api;

import xapi.fu.itr.SizedIterable;

public interface HasTypeParams {

  SizedIterable<IsTypeParameter> getTypeParams();

  default IsTypeParameter getTypeParam(String name) {
      return getTypeParams()
          .filterMapped(IsTypeParameter::getName, name::equals)
          .firstOrNull();
  }
  default boolean hasTypeParams() {
      return getTypeParams().isNotEmpty();
  }

}

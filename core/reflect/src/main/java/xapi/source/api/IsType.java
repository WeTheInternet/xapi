package xapi.source.api;

import xapi.fu.Maybe;
import xapi.fu.X_Fu;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;
import xapi.source.impl.ImmutableParameterizedType;
import xapi.source.impl.ImmutableType;

/**
 * The basic interface of any type;
 * ideally you prefer IsTypeDeclaration, or IsTypeArgument
 *
 * We should create IsGenericType for types with type parameters,
 * and IsParameterizedType, for types with type arguments.
 *
 *
 */
public interface IsType
extends HasQualifiedName
{

  boolean isPrimitive();
  IsType getEnclosingType();

  default Maybe<HasTypeParams> ifTypeParams() {
    return Maybe.not();
  }

  default Maybe<IsParameterizedType> ifParameterized() {
    return Maybe.not();
  }

  default Maybe<HasBounds> ifBounds() {
    return Maybe.not();
  }

  default IsType withTypeParams(IsTypeParameter ... params) {
    if (X_Fu.isEmpty(params)) {
      return this;
    }
    final Maybe<HasTypeParams> mine = ifTypeParams();
    if (mine.isPresent()) {
      // combine; prefer values from parameters by putting ours in the map first.
      MapLike<String, IsTypeParameter> combined = X_Jdk.mapOrderedInsertion();
      combined.putFromValues(mine.get().getTypeParams(), IsTypeParameter::getName);
      combined.putFromValues(IsTypeParameter::getName, params);
      return new ImmutableType(getPackage(), getEnclosedName(),
          combined.mappedValues().toArray(IsTypeParameter.class));
    } else {
      // just create...
      return new ImmutableType(getPackage(), getEnclosedName(), params);
    }
  }

  default IsType withTypeBounds(IsTypeArgument ... args) {
    if (X_Fu.isEmpty(args)) {
      return this;
    }
    return new ImmutableParameterizedType(null, this, args);
  }

  IsType getRawType();

  default String toSource() {
    return getQualifiedName();
  }
}

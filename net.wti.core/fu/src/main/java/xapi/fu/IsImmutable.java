package xapi.fu;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
public interface IsImmutable extends HasMutability {

  static boolean isImmutable(Out1<?> value) {
      if (value instanceof IsImmutable) {
        return value.isImmutable();
      }
      return false;
  }

  @Override
  default boolean isImmutable() {
    return true;
  }

  @Override
  default boolean isMutable() {
    return false;
  }

  default boolean immutable() {
    final Object o = this;// try not to inline this;
    // compilers that are good at inlining will inline it,
    // and those that aren't might not like `this` references in default methods...
    return o instanceof Immutable;
  }

  default Immutable asImmutable() {
    final Object o = this;
    return o instanceof Immutable ? (Immutable)o : Immutable.immutable1(o);
  }
}

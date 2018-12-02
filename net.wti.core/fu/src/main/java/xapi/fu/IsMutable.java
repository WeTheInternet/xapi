package xapi.fu;

/**
 * Created by James X. Nelson on 6/18/16.
 */
public interface IsMutable {

  default boolean mutable() {
    final Object o = this;// try not to inline this;
    // compilers that are good at inlining will inline it,
    // and those that aren't might not like `this` references in default methods...
    return o instanceof Mutable;
  }

}

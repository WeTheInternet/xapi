package xapi.fu;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
public interface IsImmutable {

  default boolean immutable() {
    final Object o = this;// try not to inline this;
    // compilers that are good at inlining will inline it,
    // and those that aren't might not like `this` references in default methods...
    return o instanceof Immutable;
  }

}

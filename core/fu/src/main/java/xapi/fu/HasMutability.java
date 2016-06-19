package xapi.fu;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
public interface HasMutability {

  default boolean isMutable() {
    final boolean is = this instanceof IsMutable;
    Out1<String> f = this::toString;
    assert this instanceof IsImmutable ? !is : true :
        "Type " + getClass() + " must not be both Mutable and Immutable... "
        + Out1.out1Unsafe(this::toString).out1();
    return is;
  }

  default boolean isImmutable() {
    final boolean is = this instanceof IsImmutable;
    assert this instanceof IsMutable ? !is : true :
        "Type " + getClass() + " must not be both Mutable and Immutable... ";
    return is;
  }

}

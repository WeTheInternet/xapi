package xapi.fu;

import static xapi.fu.Out2.out2;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
public class Immutable<O> implements Out1<O>, IsImmutable {

  private final O value;

  public Immutable(O value) {
    this.value = value;
  }

  public static <O> Immutable<O> immutable1(O of) {
    return new Immutable<>(of);
  }

  public static <O1, O2> Immutable<Out2<O1, O2>> immutable2(O1 o1, O2 o2) {
    return new Immutable<>(Out2.out2Immutable(o1, o2));
  }

  @Override
  public boolean immutable() {
    return true;
  }

  @Override
  public O out1() {
    return value;
  }

  public Immutable<O> ifNullUse(O maybeNull) {
    if (value == null) {
      return new Immutable<>(maybeNull);
    }
    return this;
  }

  public Immutable<O> ifNullThen(Out1<O> provider) {
    if (value == null) {
      return new Immutable<>(provider.out1());
    }
    return this;
  }

  public <To> Immutable<To> mapped(In1Out1<O, To> mapper) {
    final To result = mapper.io(value);
    return immutable1(result);
  }

  public <To> Immutable<To> mappedDeferred(In1Out1<O, To> mapper, Out1<O> supplier) {
    final To result = mapper.io(value);
    return immutable1(result);
  }
}

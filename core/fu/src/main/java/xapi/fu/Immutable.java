package xapi.fu;

import xapi.fu.iterate.ArrayIterable;

import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
@SuppressWarnings("all")
public class Immutable<O> implements Out1<O>, IsImmutable {

  private final O value;

  public Immutable(O value) {
    this.value = value;
  }

  public static <O> Immutable<O> immutable1(O of) {
    return new Immutable<>(of);
  }

  public static class ImmutableCompressor {

    private final Out1[] items;

    protected ImmutableCompressor(Out1[] items) {
      this.items = items;
    }
    public ImmutableCompressor(Object ... items) {

      this.items = items == null
          ? X_Fu.emptyArray()
          : ArrayIterable.<Object>iterate(items)
              .map(Immutable::immutable1)
              .toArray(Immutable[]::new);

    }

    public <O> Out1<O> compress1() {
      return Immutable.from1(items[0]);
    }

    public <S, O1 extends S, O2 extends S, O extends Out2<O1, O2> & IsImmutable> O compress2() {
      return (O)(IsImmutable & Out2<O1, O2>)()->items;
    }
  }

  public static <O> Out1<O> from1(Out1<O> item) {
    return item instanceof IsImmutable ? item : new Immutable<O>(item.out1());
  }

  public static <O1, O2> Out2<O1, O2> from2(Out2<O1, O2> item) {
    return item instanceof IsImmutable ? item : immutable2(item.out1(), item.out2());
  }

  public static <O1, O2> Out2<O1, O2> from2(Out1<O1> item1, Out1<O2> item2) {
    return new ImmutableCompressor(new Out1[]{
        from1(item1), from1(item2)
    }).compress2();
  }

  public static <O1, O2> Out2<O1, O2> immutable2(O1 o1, O2 o2) {
    return new ImmutableCompressor(o1, o2).compress2();
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

  private Object writeReplace() {
    return new ImmutableSerialized<>().setValue(value);
  }

}
class ImmutableSerialized <O> implements Serializable, IsImmutable {
  private O value;

  public O getValue() {
    return value;
  }

  public ImmutableSerialized<O> setValue(O value) {
    this.value = value;
    return this;
  }

  public Object readResolve() {
    return new Immutable<>(value);
  }

}

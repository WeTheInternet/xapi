package xapi.fu;

import xapi.fu.In1.In1Unsafe;
import xapi.fu.Out1.Out1Unsafe;

import static xapi.fu.Immutable.immutable1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
public class Mutable <I> implements In1Unsafe<I>, Out1Unsafe<I> {

  public static <Value, Bound extends Value> Mutable<Value> mutable(Class<Bound> bounds) {
    return new TypesafeMutable<>(bounds);
  }

  private volatile I value;

  public Mutable() {
  }

  public Mutable(I value) {
    this.value = value;
  }

  public Immutable<I> freeze() {
    return immutable1(value);
  }

  @Override
  public final void in(I in) {
    In1Unsafe.super.in(in);
  }

  @Override
  public final I out1() {
    return Out1Unsafe.super.out1();
  }

  @Override
  public void inUnsafe(I in) throws Throwable {
    this.value = in;
  }

  public final boolean isNull() {
    return value == null;
  }

  public final boolean isNonNull() {
    return value != null;
  }
  @Override
  public I outUnsafe() throws Throwable {
    return value;
  }

  public static class TypesafeMutable <T> extends Mutable <T> {

    private final Class<? extends T> bounds;

    public <Bound extends T> TypesafeMutable(Class<Bound> cls) {
      this.bounds = cls;
    }

    public Class<? extends T> getBounds() {
      return bounds;
    }

    @Override
    public final void inUnsafe(T in) throws Throwable {
      if (in != null) {
        in = checkInput(bounds, in);
      }
      super.inUnsafe(in);
    }

    protected T checkInput(Class<? extends T> bounds, T in) {
      if (in != null && !bounds.isInstance(in)) {
        throw new ClassCastException(in + " is not a " + bounds);
      }
      return in;
    }

    protected T checkOutput(Class<? extends T> bounds, T in) {
      if (in != null && !bounds.isInstance(in)) {
        throw new ClassCastException(in + " is not a " + bounds);
      }
      return in;
    }

    @Override
    public final T outUnsafe() throws Throwable {
      T output = super.outUnsafe();
      output = checkOutput(bounds, output);
      return output;
    }
  }
}

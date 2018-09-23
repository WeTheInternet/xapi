package xapi.fu;

import xapi.fu.In1.In1Unsafe;
import xapi.fu.Out1.Out1Unsafe;
import xapi.fu.api.Copyable;
import xapi.fu.has.HasLock;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
public class Mutable <T> implements In1Unsafe<T>, Out1Unsafe<T>, HasLock {

  public interface MutableAsIO <I> extends In1Out1<I, I> {

    Mutable<Out2<In1<I>, Out1<I>>> data();

    @Override
    default I io(I in) {
      final Mutable<Out2<In1<I>, Out1<I>>> wrapper = data();
      final Out2<In1<I>, Out1<I>> data = wrapper.out1();

      final I was = data.out2().out1();
      if (in != null) {
        // allow the use of null to say "only read";
        // use #clear or #replace...
        data.out1().in(in);
      }
      return was;
    }

    default void clear() {
      replace(new Mutable<>());
    }

    static <I> MutableAsIO<I> toIO(Mutable<I> mutable) {
      final Mutable<Out2<In1<I>, Out1<I>>> container = new Mutable<>();
      MutableAsIO<I> asIO = ()->container;
      return asIO.replace(mutable);
    }

    default MutableAsIO<I> replace(Out2<In1<I>, Out1<I>> pointer) {
      data().in(pointer);
      return this;
    }

    default MutableAsIO<I> replace(Mutable<I> value) {
      data().in(Out2.out2Immutable(value::in, value::out1));
      return this;
    }
  }

  public static <Value, Bound extends Value> Mutable<Value> mutable(Class<Bound> bounds) {
    return new TypesafeMutable<>(bounds);
  }

  private volatile T value;

  public Mutable() {
  }

  public Mutable(T value) {
    this.value = value;
  }

  public Immutable<T> freeze() {
    return Immutable.immutable1(value);
  }

  @Override
  public final void in(T in) {
    In1Unsafe.super.in(in);
  }

  @Override
  public final T out1() {
    return Out1Unsafe.super.out1();
  }

  public final MutableAsIO<T> asIO() {
    return MutableAsIO.toIO(this);
  }

  @Override
  public void inUnsafe(T in) throws Throwable {
    this.value = in;
  }

  @Override
  public T outUnsafe() throws Throwable {
    return value;
  }

  public T compute(In1Out1<T, T> mapper) {
    return mutex(()->{
      final T oldVal = out1();
      final T newVal = mapper.io(oldVal);
      in(newVal);
      return newVal;
    });
  }

  public final boolean isNull() {
    return value == null;
  }

  public final boolean isNonNull() {
    return value != null;
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
        in = checkInput(in);
      }
      super.inUnsafe(in);
    }

    protected T checkInput(T in) {
      checkBounds(in);
      return in;
    }

    protected T checkOutput(T in) {
      checkBounds(in);
      return in;
    }

    @Override
    public final T outUnsafe() throws Throwable {
      T output = super.outUnsafe();
      output = checkOutput(output);
      return output;
    }

    @Override
    public Mutable<T> copy() {
      T value = out1();
      if (value instanceof Copyable) {
        final Object newValue = ((Copyable) value).copy();
        checkBounds(newValue);
        value = (T) newValue;
      }
      return new Mutable<>(value);
    }

    protected void checkBounds(Object newValue) {
      if (newValue != null && !bounds.isInstance(newValue)) {
        throw new ClassCastException(newValue + " is not a " + bounds);
      }
    }
  }

    public final void set(T b) {
      in(b);
    }

    public final T setReturnNew(T b) {
      in(b);
      return b;
    }

    public final Mutable<T> process(In1Out1<T, T> mapper) {
      return mutex(()->{
        final T mapped = mapper.io(out1());
        in(mapped);
        return this;
      });
    }

    public final <P1> Mutable<T> process(In2Out1<T, P1, T> mapper, P1 param1) {
      return mutex(()->{
        final T mapped = mapper.io(out1(), param1);
        in(mapped);
        return this;
      });
    }

    public final <P1, P2> Mutable<T> process(In3Out1<T, P1, P2, T> mapper, P1 param1, P2 param2) {
      return mutex(()-> {
        final T mapped = mapper.io(out1(), param1, param2);
        in(mapped);
        return this;
      });
    }

    public final Do useThenSetLater(In1<T> callback, T newVal) {
      return ()->useThenSet(callback, newVal);
    }

    public final Mutable<T> useThenSet(In1<T> callback, T newVal) {
      mutex(()->{
        callback.in(out1());
        in(newVal);
      });
      return this;
    }

  public final T setReturnOld(T b) {
      final T old = out1();
      in(b);
      return old;
    }

    public T replace(T s) {
      T was = out1();
      in(s);
      return was;
    }

  public Mutable<T> copy() {
    T value = out1();
    if (value instanceof Copyable) {
      value = ((Copyable<T>) value).copy();
    }
    return new Mutable<>(value);
  }
}

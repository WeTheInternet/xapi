package xapi.fu;

import xapi.fu.In1.In1Unsafe;
import xapi.fu.Out1.Out1Unsafe;

import static xapi.fu.Immutable.immutable1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
public class Mutable <I> implements In1Unsafe<I>, Out1Unsafe<I> {

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

  public final MutableAsIO<I> asIO() {
    return MutableAsIO.toIO(this);
  }

  @Override
  public void inUnsafe(I in) throws Throwable {
    this.value = in;
  }

  @Override
  public I outUnsafe() throws Throwable {
    return value;
  }

  public I compute(In1Out1<I, I> mapper) {
    return mutex(()->{
      final I oldVal = out1();
      final I newVal = mapper.io(oldVal);
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

    public final void set(I b) {
      in(b);
    }

    public final I setReturnNew(I b) {
      in(b);
      return b;
    }

    public final Mutable<I> process(In1Out1<I, I> mapper) {
      final I mapped = mapper.io(out1());
      in(mapped);
      return this;
    }

    public final <P1> Mutable<I> process(In2Out1<I, P1, I> mapper, P1 param1) {
      final I mapped = mapper.io(out1(), param1);
      in(mapped);
      return this;
    }

    public final <P1, P2> Mutable<I> process(In3Out1<I, P1, P2, I> mapper, P1 param1, P2 param2) {
      final I mapped = mapper.io(out1(), param1, param2);
      in(mapped);
      return this;
    }

    public final Mutable<I> useThenSet(In1<I> callback, I newVal) {
      mutex(()->{
        callback.in(out1());
        in(newVal);
      });
      return this;
    }

  public final void mutex(Do o) {
    mutex(o.returns1(null));
  }

  public <O> O mutex(Out1<O> o) {
    synchronized (this) {
      return o.out1();
    }
  }

  public final I setReturnOld(I b) {
      final I old = out1();
      in(b);
      return old;
    }

    public I replace(I s) {
      I was = out1();
      in(s);
      return was;
    }
}

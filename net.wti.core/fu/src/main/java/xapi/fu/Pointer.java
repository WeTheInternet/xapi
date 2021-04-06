package xapi.fu;

import xapi.fu.log.Log;
import xapi.fu.log.Log.DefaultLog;
import xapi.fu.log.Log.LogLevel;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 1/3/16.
 */
public interface Pointer <T> extends In1<T>, Out1<T>, In1Out1<T, T> {

  Out2<In1<T>, Out1<T>> accessors();

  @Override
  default void in(T in) {
    accessors().out1().in(in);
  }

  @Override
  default T out1() {
    return accessors().out2().out1();
  }

  @Override
  default T io(T in) {
    return getThenSet(in);
  }

  default T getThenSet(T in) {
    T was = out1();
    in(in);
    return was;
  }

  default T getThenApply(In1Out1<T, T> io) {
    T was = out1();
    T next = io.io(was);
    in(next);
    return was;
  }

  default T setThenGet(T in) {
    io(in);
    return out1();
  }

  default Do borrow(final T value) {
    final T current = out1();

    return () -> {
      final T later = out1();
      if (later == value) {
        in(current);
      } else {
        throw new IllegalStateException("Illegal Pointer.borrow, when we borrowed " + current + " and tried to return value, pointer moved to " + later);
      }
    };
  }

  static <T> Pointer <T> pointer() {
    T[] value = X_Fu.array((T)null);
    final In1<T> in = In1.from1(X_Fu::setZeroeth, value);
    final Out1<T> out = Out1.out1Deferred(X_Fu::getZeroeth, value);
    final Out2<In1<T>, Out1<T>> pair = Out2.out2Immutable(in, out);
    return ()-> pair;
  }

  static <T> Pointer <T> pointerTo(T value) {
    final Pointer<T> pointer = pointer();
    pointer.in(value);
    return pointer;
  }

  static <T> Pointer <T> pointerJoin(In1<T> input, Out1<T> output) {
    return new PointerJoin<>(input, output);
  }

  static <T> Pointer <T> pointerDeferred(Out1<T> value) {
    return new PointerDeferred<>(value);
  }

  static <T> PointerImmutable <T> pointerImmutable(T value) {
    return new PointerImmutable<>(value);
  }

  static <T> PointerLazy <T> pointerLazy(Out1<T> value) {
    return new PointerLazy<>(value);
  }

  interface PointerComparable<T extends Comparable<T>> extends Pointer<T>, Lambda {

  }

  final class PointerJoin <T> implements Pointer<T>, DefaultLog {

    @Override
    public Out2<In1<T>, Out1<T>> accessors() {
      return Out2.out2Immutable(in, out);
    }

    private final In1<T> in;
    private final Out1<T> out;

    PointerJoin(In1<T> in, Out1<T> out) {
      this.in = in;
      this.out = out;
    }

    @Override
    public void in(T i) {
      in.in(i);
    }

    @Override
    public T out1() {
      return out.out1();
    }
  }
  class PointerDeferred <T> implements Pointer<T>, DefaultLog {
    protected volatile Out1<T> value;

    public PointerDeferred(Out1<T> value) {
      this.value = value;
    }

    public PointerDeferred() {
      value = this::warnUninitialized;
    }

    protected T warnUninitialized() {
      log(getClass(), "Attempting to get the value from a PointerDeferred, ", this, ", before its value has been initialized");
      return null;
    }

    @Override
    public Out2<In1<T>, Out1<T>> accessors() {
      return Out2.out2Immutable(this, this);
    }

    @Override
    public void in(T in) {
      this.value = Immutable.immutable1(in);
    }

    public void supply(Out1<T> deferred) {
      this.value = deferred;
    }

    @Override
    public T out1() {
      return value.out1();
    }
  }

  class PointerLazy <T> extends PointerDeferred <T> implements IsLazy {

    PointerLazy() {}
    PointerLazy(Out1<T> of) {value = of;}

    @Override
    public T out1() {
      if (value instanceof Immutable) {
        return value.out1();
      }
      T result = value.out1();
      value = Immutable.immutable1(result);
      return result;
    }
  }

  class PointerImmutable <T> implements Pointer<T>, IsImmutable {
    private final T value;

    public PointerImmutable(T value) {
      this.value = value;
    }

    @Override
    public void in(T in) {
      warnAttemptToSet(in);
    }

    protected void warnAttemptToSet(T in) {
      if ("true".equals(System.getProperty("xapi.strict.immutable"))) {
        final Out1<String> message = Immutable.immutable1("Attempting to assign value [" + in + "] to an immutable pointer: " + getClass() + " : " + this);
        Log.firstLog(in, this).log(LogLevel.DEBUG, "Attempt to set an immutable");
        assert false : message;
      }
    }

    @Override
    public Out2<In1<T>, Out1<T>> accessors() {
      return Out2.out2Immutable(this, this);
    }

    @Override
    public T out1() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof PointerImmutable))
        return false;

      final PointerImmutable<?> that = (PointerImmutable<?>) o;

      return X_Fu.equal(value, that.value);

    }

    @Override
    public int hashCode() {
      return value != null ? value.hashCode() : 0;
    }
  }

}

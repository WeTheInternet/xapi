package xapi.fu;

import xapi.fu.Filter.Filter1;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@FunctionalInterface
@SuppressWarnings("unchecked") // yes, this api will let you do terrible things.  Don't do terrible things.
public interface In1<I> extends HasInput, Rethrowable, Lambda {

  In1 IGNORED = new In1() {
    @Override
    public void in(Object ignored) {}

    @Override
    public In1 useAfterMe(In1 next) {
      return next;
    }

    @Override
    public In1 useBeforeMe(In1 next) {
      return next;
    }

  };

  static <T> In1<T> ignored() {
    return IGNORED;
  }

  void in(I in);

  default In1<I> inChain(I in) {
    in(in);
    return this;
  }

  default void applyAfterMe(I in, In1<I> next) {
    in(in);
    next.in(in);
  }

  default void applyAfterMeUnsafe(I in, In1Unsafe<I> next) {
    in(in);
    next.in(in);
  }

  default void applyBeforeMe(I in, In1<I> next) {
    next.in(in);
    in(in);
  }

  default void applyBeforeMeUnsafe(I in, In1Unsafe<I> next) {
    next.in(in);
    in(in);
  }

  default In1<I> useAfterMe(In1<I> next) {
    if (next == null || next == IGNORED) {
      return this;
    }
    return in->{
      in(in);
      next.in(in);
    };
  }

  default In1Unsafe<I> useAfterMeUnsafe(In1Unsafe<I> next) {
    if (next == null || next == IGNORED) {
      return unsafe();
    }
    return in->{
      in(in);
      next.in(in);
    };
  }

  default In1<I> useBeforeMe(In1<I> next) {
    if (next == null || next == IGNORED) {
      return this;
    }
    return in -> {
      next.in(in);
      in(in);
    };
  }

  default In1Unsafe <I> useBeforeMeUnsafe(In1Unsafe<I> next) {
    return in->{
      next.in(in);
      in(in);
    };
  }

  default In1Unsafe<I> unsafe() {
    if (this instanceof In1Unsafe) {
      return (In1Unsafe<I>) this;
    }
    return this::in;
  }

  @Override
  default int accept(int position, Object... values) {
    in((I) values[position++]);
    return position;
  }

  default Consumer<I> toConsumer() {
    return this instanceof Consumer ? (Consumer<I>)this : this::in;
  }

  default Do provide(I in) {
    return ()->in(in);
  }

  default Do provideDeferred(Out1<I> in) {
    return ()->in(in.out1());
  }

  default <N> In2<N, I> requireBefore(In1<N> and) {
    return In2.in2(and, this);
  }

  default <N> In2<I, N> requireAfter(In1<N> and) {
    return In2.in2(this, and);
  }

  static <I> In1<I> in1(Consumer<I> of) {
    return of::accept;
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions,
   * but exposes an exceptionless api.  If you don't have to call code with checked exceptions,
   * prefer the standard {@link #in1(Consumer)}, as try/catch can disable / weaken some JIT compilers.
   */
  static <I> In1<I> in1Unsafe(In1Unsafe<I> of) {
    return of;
  }

  static <I> In1<I> noop() {
    return ignored->{};
  }

  interface In1Unsafe <I> extends In1<I> {
    void inUnsafe(I in) throws Throwable;

    default void in(I in) {
      try {
        inUnsafe(in);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }

  static <I1, I2> In1<I1> from2(In2<? super I1, ? super I2> adapt, I2 i2) {
    return i1 -> adapt.in(i1, i2);
  }

  static <I1, I2> In1<I1> mapped1(In2<I1, I2> adapt, In1Out1<I1, I2> mapper) {
    return i1 -> adapt.in(i1, mapper.io(i1));
  }

  static <I1, I2> In1<I1> mapped1Reverse(In1Out1<I1, I2> mapper, In2<I1, I2> operation) {
    return i1 -> {
      final I2 mapped = mapper.io(i1);
      operation.in(i1, mapped);
    };
  }

  static <I1, I2> In1<I2> mapped2(In2<I1, I2> adapt, In1Out1<I2, I1> i1) {
    return i2 -> adapt.in(i1.io(i2), i2);
  }

  static <I1, I2> In1<I2> mapped2Reverse(In1Out1<I2, I1> mapper, In2<I1, I2> adapt) {
    return i2 -> {
      final I1 mapped = mapper.io(i2);
      adapt.in(mapped, i2);
    };
  }

  static <I1, I2> In1<I2> from1(In2<I1, I2> adapt, I1 i1) {
    return i2 -> adapt.in(i1, i2);
  }

  static <I2, I3> In1<In2<I2, I3>> in2Adapter(I2 i2, I3 i3) {
    return from23(In2::in, i2, i3);
  }

  static <I1, I2, I3> In1<I1> from23(In3<I1, I2, I3> adapt, I2 i2, I3 i3) {
    return i1 -> adapt.in(i1, i2, i3);
  }

  static <I1, I2, I3> In1<I3> from12(In3<I1, I2, I3> adapt, I1 i1, I2 i2) {
    return i3 -> adapt.in(i1, i2, i3);
  }

  static <I1, I2, I3> In1<I2> from13(In3<I1, I2, I3> adapt, I1 i1, I3 i3) {
    return i2 -> adapt.in(i1, i2, i3);
  }

  static <I1> In1<In1<I1>> receiver(I1 value) {
    // We need to declare a variable with this type for type inference to work.
    final In2<In1<I1>, I1> in2 = In1::in;
    return in2.provide2(value);
  }

  static <I> In1<I> ignoreIn1(Runnable r) {
    return ignored -> r.run();
  }

  default void forEach(Iterable<I> value) {
    value.forEach(toConsumer());
  }

  default <To> In1<To> map1(In1Out1<To, I> mapper) {
    return i1->in(mapper.io(i1));
  }

  static <E> In1Serializable<E> serializable1(In1<E> from) {
    return from::in;
  }

  interface In1Serializable <I> extends In1<I>, Serializable { }

  default  <V> In2<I, V> ignore2() {
    return (i, ignored) -> in(i);
  }

  default  <V> In2<V, I> ignore1() {
    return (ignored, i) -> in(i);
  }

  default Filter1<I> filtered(Filter1<I> filter) {
    return i-> {
      if (filter.filter1(i)) {
        in(i);
        return true;
      }
      return false;
    };
  }

  default In1<I> onlyOnce() {
    final In1<I>[] once = new In1[]{this};
    return new In1<I>() {
      @Override
      public void in(I in) {
        final In1<I> was;
        synchronized (once) {
          was = once[0];
          // TODO: make once[0] a temporary blocker, and only set to IGNORED after completion.
          // We still want to avoid running foreign code inside any monitors,
          // as we don't want to contribute to any deadlock soup.
          once[0] = In1.IGNORED;
        }
        was.in(in);
      }

      @Override
      public In1<I> useAfterMe(In1<I> next) {
        if (once[0] == In1.IGNORED) {
          return next;
        }
        return In1.super.useAfterMe(next);
      }

      @Override
      public In1<I> useBeforeMe(In1<I> next) {
        if (once[0] == In1.IGNORED) {
          return next;
        }
        return In1.super.useBeforeMe(next);
      }
    };
  }

    default In1Out1<I, I> returnArg() {
      return i->{
        in(i);
        return i;
      };
    }

    static <ITo, IFrom> In1<ITo> in1Mapped(In1<IFrom> onInitialize, In1Out1<ITo, IFrom> o) {
      return i -> {
        IFrom b = o.io(i);
        onInitialize.in(b);
      };
    }

    static <I1> In1<I1> in1Ignored(In1<I1> onInitialize, Out1<I1> o) {
      return i -> {
        I1 b = o.out1();
        onInitialize.in(b);
      };
    }

  static <T> In2<In1<T>, T> invoker() {
    return In1::in;
  }
}

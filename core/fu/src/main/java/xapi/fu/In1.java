package xapi.fu;

import java.util.function.Consumer;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked") // yes, this api will let you do terrible things.  Don't do terrible things.
public interface In1<I> extends HasInput {

  void in(I in);

  @Override
  default int accept(int position, Object... values) {
    in((I) values[position++]);
    return position;
  }

  default Consumer<I> toConsumer() {
    return this::in;
  }

  default Do provide(I in) {
    return ()->in(in);
  }

  default Do provide(Out1<I> in) {
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

  interface In1Unsafe <I> extends In1<I>, Rethrowable{
    void inUnsafe(I in) throws Throwable;

    default void in(I in) {
      try {
        inUnsafe(in);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }
}

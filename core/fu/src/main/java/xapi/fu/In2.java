package xapi.fu;

import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked")
public interface In2<I1, I2> extends HasInput, Rethrowable {

  void in(I1 in1, I2 in2);

  @Override
  default int accept(int position, Object... values) {
    in((I1)values[position++], (I2)values[position++]);
    return position;
  }

  default BiConsumer<I1, I2> toBiConsumer() {
    return this::in;
  }

  default <N> In3<N, I1, I2> requireBefore(In1<N> and){
    return In3.in3(and, this);
  }

  default <N> In3<I1, I2, N> requireAfter(In1<N> and){
    return In3.in3(this, and);
  }

  default In2<I2, I1> reverse(){
    return (i2, i1) -> in(i1, i2);
  }

  default In1<I2> provide1(Out1<I1> and){
    return i2 -> in(and.out1(), i2);
  }

  default In1<I1> provide2(Out1<I2> and){
    return i1 -> in(i1, and.out1());
  }

  default In1<I2> provide1(I1 and){
    return i2 -> in(and, i2);
  }

  default In1<I1> provide2(I2 and){
    return i1 -> in(i1, and);
  }

  static <I1, I2> In2<I1, I2> in2(BiConsumer<I1, I2> of) {
    return of::accept;
  }

  static <I1, I2> In2<I1, I2> in2(In1<I1> in1, In1<I2> in2) {
    return (i1, i2)->{
      in1.in(i1);
      in2.in(i2);
    };
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions,
   * but exposes an exceptionless api.  If you don't have to call code with checked exceptions,
   * prefer the standard {@link #in2(BiConsumer)}, as try/catch can disable / weaken some JIT compilers.
   */
  static <I1, I2> In2<I1, I2> in1Unsafe(In2Unsafe<I1, I2> of) {
    return of;
  }

  default <W> In1 <W> adapt(In1Out1<W, I1> getter1, In1Out1<W, I2> getter2) {
    return w->in(getter1.io(w), getter2.io(w));
  }

  default Consumer<Entry<I1, I2>> mapAdapter() {
    return e->in(e.getKey(), e.getValue());
  }

  interface In2Unsafe <I1, I2> extends In2<I1, I2> {
    void inUnsafe(I1 in1, I2 in2) throws Throwable;

    default void in(I1 in1, I2 in2) {
      try {
        inUnsafe(in1, in2);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }
}

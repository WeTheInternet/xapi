package xapi.fu;

import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked")
public interface In2<I1, I2> extends HasInput, Rethrowable, Lambda {

    In2 NULL = (ig,nore)->{};

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

  default In1<I2> adapt1(In1Out1<I2, I1> adapt) {
    return i2 -> in(adapt.io(i2), i2);
  }

  default In1<I1> adapt2(In1Out1<I1, I2> adapt) {
    return i1 -> in(i1, adapt.io(i1));
  }

  default <In extends I1> In1<I2> provide1(In and){
    return i2 -> in(and, i2);
  }

  default In1<I2> provide1Deferred(Out1<I1> and){
    return i2 -> {
      final I1 i1 = and.out1();
      in(i1, i2);
    };
  }

  default In1<I2> provide1Immediate(Out1<I1> and){
    final I1 i1 = and.out1();
    return i2 -> in(i1, i2);
  }

  default In1<I1> provide2(I2 and){
    return i1 -> in(i1, and);
  }

  default In1<I1> provide2Deferred(Out1<I2> and){
    return i1 -> {
      final I2 i2 = and.out1();
      in(i1, i2);
    };
  }

  default In1<I1> provide2Immediate(Out1<I2> and){
    final I2 i2 = and.out1();
    return i1 -> in(i1, i2);
  }


  static <I1, I2> In2<I1, I2> in2(In2<I1, I2> of) {
    return of;
  }

  static <I1, I2> In2<I1, I2> fromBiconsumer(BiConsumer<I1, I2> of) {
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
   * prefer the standard {@link #fromBiconsumer(BiConsumer)}, as try/catch can disable / weaken some JIT compilers.
   */
  static <I1, I2> In2<I1, I2> in2Unsafe(In2Unsafe<I1, I2> of) {
    return of;
  }

  default <W> In1 <W> adapt(In1Out1<W, I1> getter1, In1Out1<W, I2> getter2) {
    return w->in(getter1.io(w), getter2.io(w));
  }

  default <To> In2<To, I2> map1 (In1Out1<To, I1> mapper) {
    return (i1, i2) -> in(mapper.io(i1), i2);
  }

  default <To> In2<I1, To> map2 (In1Out1<To, I2> mapper) {
    return (i1, i2) -> in(i1, mapper.io(i2));
  }

  default Consumer<Entry<I1, I2>> mapAdapter() {
    // You can't overload the same type to have different generics in a single expression,
    // so we can't actually have two different In1Out generics in an inline expression
    In1Out1<Entry<I1, I2>, I1> key = Entry::getKey;
    In1Out1<Entry<I1, I2>, I2> value = Entry::getValue;
    return adapt(key, value).toConsumer();
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

  static <I1, I2> In2<I1, I2> ignoreSecond(In1<I1> callback) {
    return (i1, i2) -> callback.in(i1);
  }

  static <I1, I2> In2<I1, I2> ignoreFirst(In1<I2> callback) {
    return (i1, i2) -> callback.in(i2);
  }

  static <I1, I2> In2<I2, I1> reversed(In2<I1, I2> from) {
    return (i1, i2) -> from.in(i2, i1);
  }

  default <O1> In2Out1<I1, I2, O1> supply1(O1 value) {
    return (i1, i2) -> {
      in(i1, i2);
      return value;
    };
  }

  default <O1> In2Out1<I1, I2, O1> supply1Immediate(Out1<O1> factory) {
    final O1 value = factory.out1();
    return (i1, i2) -> {
      in(i1, i2);
      return value;
    };
  }

  default <O1> In2Out1<I1, I2, O1> supply1BeforeRead(Out1<O1> factory) {
    return (i1, i2) -> {
      final O1 value = factory.out1();
      in(i1, i2);
      return value;
    };
  }

  default In2<I1, I2> doBeforeMe(In2<I1, I2> other) {
    return (i1, i2) -> {
      other.in(i1, i2);
      in(i1, i2);
    };
  }

  default In2<I1, I2> doAfterMe(In2<I1, I2> other) {
    return (i1, i2) -> {
      in(i1, i2);
      other.in(i1, i2);
    };
  }

  default <O1> In2Out1<I1, I2, O1> supply1AfterRead(Out1<O1> factory) {
    return (i1, i2) -> {
      in(i1, i2);
      final O1 value = factory.out1();
      return value;
    };
  }

  static <I1, I2> Do reduceAll(In2<I1, I2> lambda, I1 i1, I2 i2) {
    return lambda.provide1(i1).provide(i2);
  }

  static <I1, I2> In1<I2> reduce1(In2<I1, I2> lambda, I1 i1) {
    return lambda.provide1(i1);
  }

  static <I1, I2> In1<I1> reduce2(In2<I1, I2> lambda, I2 i2) {
    return lambda.provide2(i2);
  }

  default <I> In3<I, I1, I2> ignore1() {
    return (i, i1, i2) -> in(i1, i2);
  }

  default <I> In3<I1, I, I2> ignore2() {
    return (i1, i, i2) -> in(i1, i2);
  }

  default <I> In3<I1, I2, I> ignore3() {
    return (i1, i2, i) -> in(i1, i2);
  }

  default In2<I1, I2> onlyOnce() {
    final In2<I1, I2>[] self = new In2[]{this};
    return (i1, i2) -> {
      final In2<I1, I2> was;
      synchronized (self) {
        was = self[0];
        self[0] = NULL;
      }
      was.in(i1, i2);
    };
  }
}

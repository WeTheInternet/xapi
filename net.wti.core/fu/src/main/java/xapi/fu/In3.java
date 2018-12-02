package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked")
@FunctionalInterface

public interface In3<I1, I2, I3> extends HasInput, Rethrowable, Lambda {

  void in(I1 in1, I2 in2, I3 in3);

  In3 NULL = (ig,no,re)->{};

  In3<In2, ?, ?> INVOKE_IN2 = In2::in;

  static <I1, I2> In3<In2<I1, I2>, I1, I2> invokeIn2() {
    In3 in = INVOKE_IN2;
    return in;
  }


  @Override
  default int accept(int position, Object... values) {
    in((I1)values[position++], (I2)values[position++], (I3)values[position++]);
    return position;
  }

  static <I1, I2, I3> In3<I1, I2, I3> in3(In3<I1, I2, I3> of) {
    return of;
  }

  default <N> In4<N, I1, I2, I3> requireBefore(In1<N> and){
    return In4.in4(and, this);
  }

  default <N> In4<I1, I2, I3, N> requireAfter(In1<N> and){
    return In4.in4(this, and);
  }

  default In3<I3, I2, I1> reverse(){
    return  (i3, i2, i1) -> in(i1, i2, i3);
  }

  default In3<I3, I1, I2> rotateRight(){
    return  (i3, i1, i2) -> in(i1, i2, i3);
  }

  default In3<I2, I3, I1> rotateLeft(){
    return  (i2, i3, i1) -> in(i1, i2, i3);
  }

  default In2<I2, I3> provide1(Out1<I1> and){
    return (i2, i3) -> in(and.out1(), i2, i3);
  }

  default In2<I1, I3> provide2(Out1<I2> and){
    return (i1, i3) -> in(i1, and.out1(), i3);
  }

  default In2<I1, I2> provide3(Out1<I3> and){
    return (i1, i2) -> in(i1, i2, and.out1());
  }

  default <T> In3<T, I2, I3> map1(In1Out1<T, I1> mapper) {
    return (to, i2, i3)->in(mapper.io(to), i2, i3);
  }

  default <T> In3<I1, T, I3> map2(In1Out1<T, I2> mapper) {
    return (i1, to, i3)->in(i1, mapper.io(to), i3);
  }

  default <T> In3<I1, I2, T> map3(In1Out1<T, I3> mapper) {
    return (i1, i2, to)->in(i1, i2, mapper.io(to));
  }

  default In2<I2, I3> provide1(I1 and){
    return (i2, i3) -> in(and, i2, i3);
  }

  default In2<I1, I3> provide2(I2 and){
    return (i1, i3) -> in(i1, and, i3);
  }

  default In2<I1, I2> provide3(I3 and){
    return (i1, i2) -> in(i1, i2, and);
  }


  default In2<I1, I3> adapt2(In1Out1<I1, I2> adapt) {
    return (i1, i3) -> in(i1, adapt.io(i1), i3);
  }

  default In2<I1, I2> adapt3(In1Out1<I1, I3> adapt) {
    return (i1, i2) -> in(i1, i2, adapt.io(i1));
  }

  static <I1, I2, I3> In3<I1, I2, I3> in3(In2<I1, I2> in12, In1<I3> in3) {
    return (i1, i2, i3)-> {
      in12.in(i1, i2);
      in3.in(i3);
    };
  }

  static <I1, I2, I3> In3<I1, I2, I3> in3(In1<I1> in1, In2<I2, I3> in23) {
    return (i1, i2, i3)-> {
      in1.in(i1);
      in23.in(i2, i3);
    };
  }

  static <I1, I2, I3> In3<I1, I2, I3> in3(In1<I1> in1, In1<I2> in2, In1<I3> in3) {
    return (i1, i2, i3)-> {
      in1.in(i1);
      in2.in(i2);
      in3.in(i3);
    };
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions,
   * but exposes an exceptionless api.  If you don't have to call code with checked exceptions,
   * prefer the standard {@link #in3(In3)}, as try/catch can disable / weaken some JIT compilers.
   */
  static <I1, I2, I3> In3<I1, I2, I3> in3Unsafe(In3Unsafe<I1, I2, I3> of) {
    return of;
  }

  interface In3Unsafe <I1, I2, I3> extends In3<I1, I2, I3> {
    void inUnsafe(I1 in1, I2 in2, I3 in3) throws Throwable;

    default void in(I1 in1, I2 in2, I3 in3) {
      try {
        inUnsafe(in1, in2, in3);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }

    static <I1, I2, I3> In3<I1, I2, I3> ignored() {
      return NULL;
    }
}

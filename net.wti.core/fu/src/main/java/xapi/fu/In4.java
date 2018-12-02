package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked")
public interface In4<I1, I2, I3, I4> extends HasInput, Rethrowable, Lambda {

  void in(I1 in1, I2 in2, I3 in3, I4 in4);

  @Override
  default int accept(int position, Object... values) {
    in((I1)values[position++],(I2)values[position++],(I3)values[position++],(I4)values[position++]);
    return position;
  }

  default <N> InMany requireBefore(In1<N> and){
    return InMany.of(and, this);
  }

  default <N> InMany requireAfter(In1<N> and){
    return InMany.of(this, and);
  }

  default In4<I4, I3, I2, I1> reverse(){
    return  (i4, i3, i2, i1) -> in(i1, i2, i3, i4);
  }

  default In4<I4, I1, I2, I3> rotateRight(){
    return  (i4, i1, i2, i3) -> in(i1, i2, i3, i4);
  }

  default In4<I2, I3, I4, I1> rotateLeft(){
    return  (i2, i3, i4, i1) -> in(i1, i2, i3, i4);
  }

  default In3<I2, I3, I4> provide1(I1 and){
    return (i2, i3, i4) -> in(and, i2, i3, i4);
  }

  default In3<I1, I3, I4> provide2(I2 and){
    return (i1, i3, i4) -> in(i1, and, i3, i4);
  }

  default In3<I1, I2, I4> provide3(I3 and){
    return (i1, i2, i4) -> in(i1, i2, and, i4);
  }

  default In3<I1, I2, I3> provide4(I4 and){
    return (i1, i2, i3) -> in(i1, i2, i3, and);
  }

  static <I1, I2, I3, I4> In4<I1, I2, I3, I4> in4(In4<I1, I2, I3, I4> of) {
    return of;
  }

  static <I1, I2, I3, I4> In4<I1, I2, I3, I4> in4(In3<I1, I2, I3> in123, In1<I4> in4) {
    return (i1, i2, i3, i4) -> {
      in123.in(i1, i2, i3);
      in4.in(i4);
    };
  }

  static <I1, I2, I3, I4> In4<I1, I2, I3, I4> in4(In2<I1, I2> in12, In2<I3, I4> in34) {
    return (i1, i2, i3, i4) -> {
      in12.in(i1, i2);
      in34.in(i3, i4);
    };
  }

  static <I1, I2, I3, I4> In4<I1, I2, I3, I4> in4(In1<I1> in1, In3<I2, I3, I4> in234) {
    return (i1, i2, i3, i4) -> {
      in1.in(i1);
      in234.in(i2, i3, i4);
    };
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions,
   * but exposes an exceptionless api.  If you don't have to call code with checked exceptions,
   * prefer the standard {@link #in4(In4)}, as try/catch can disable / weaken some JIT compilers.
   */
  static <I1, I2, I3, I4> In4<I1, I2, I3, I4> in4Unsafe(In4Unsafe<I1, I2, I3, I4> of) {
    return of;
  }

  interface In4Unsafe <I1, I2, I3, I4> extends In4<I1, I2, I3, I4> {
    void inUnsafe(I1 in1, I2 in2, I3 in3, I4 in4) throws Throwable;

    default void in(I1 in1, I2 in2, I3 in3, I4 in4) {
      try {
        inUnsafe(in1, in2, in3, in4);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }
}

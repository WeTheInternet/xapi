package xapi.fu;

/**
 * A marker interface we apply to immutable types.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Do {

  void done();

  default Runnable toRunnable() {
    return this::done;
  }

  default <I> In1<I> requireBefore(In1<I> in1) {
    return i->{
      in1.in(i);
      done();
    };
  }

  default <I> In1<I> requireAfter(In1<I> in1) {
    return i->{
      done();
      in1.in(i);
    };
  }

  static Do of(Runnable r) {
    return r::run;
  }

  static Do ofUnsafe(Runnable r) {
    return r::run;
  }

  interface DoUnsafe extends Do, Rethrowable{
    void doneUnsafe() throws Throwable;

    default void in() {
      try {
        doneUnsafe();
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }

}

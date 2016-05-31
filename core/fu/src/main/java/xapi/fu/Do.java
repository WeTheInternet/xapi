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

  static <V> void forEach(Iterable<V> values, In1<V> job) {
    values.forEach(job.toConsumer());
  }

  static <V, To> void forEachMapped2(Iterable<V> values, In1Out1<V, To> mapper, In2<V, To> job) {
    forEach(values, In1.mapped1(job, mapper));
  }

  static <V, To> void forEachMapped1(Iterable<V> values, In1Out1<V, To> mapper, In2<To, V> job) {
    forEach(values, In1.mapped2(job, mapper));
  }

}

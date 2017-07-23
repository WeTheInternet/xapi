package xapi.fu;

/**
 * A marker interface we apply to immutable types.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Do extends AutoCloseable {

  Do NOTHING = new Do() {
    @Override
    public void done() {
    }

    @Override
    public Do doBefore(Do d) {
      return d;
    }

    @Override
    public Do doAfter(Do d) {
      return d;
    }
  };

    void done();

  default Runnable toRunnable() {
    return this::done;
  }

  default Do doBefore(Do d) {
    return ()->{
      d.done();
      done();
    };
  }

  @Override
  default void close() { // erases exceptions
    done();
  }

  default Do doAfter(Do d) {
    return ()->{
      done();
      d.done();
    };
  }

  default <I> In1<I> requireBefore(In1<I> in1) {
    return i->{
      in1.in(i);
      done();
    };
  }

  default <I> In1<I> ignores1() {
    return ignored->done();
  }
  default <O> Out1<O> returns1(O val) {
    return ()->{
      done();
      return val;
    };
  }

  default <O> Out1<O> returns1Deferred(Out1<O> val) {
    return ()->{
      done();
      return val.out1();
    };
  }

  default <O> Out1<O> returns1Immediate(Out1<O> val) {
    O value = val.out1();
    return ()->{
      done();
      return value;
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

    @Override
    default void done() {
      try {
        doneUnsafe();
      } catch (Throwable throwable) {
        throw rethrow(throwable);
      }
    }

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

  default Do onlyOnce() {
    return new DoOnce(this);
  }

}
final class DoOnce implements Do {

  private final Mutable<Do> todo;

  public DoOnce(Do onlyOnce) {
    todo = new Mutable<>(onlyOnce);
  }

  @Override
  public final Do onlyOnce() {
    return this; // no need to double-wrap
  }

  @Override
  public void done() {
    todo.useThenSet(Do::done, Do.NOTHING);
  }
}

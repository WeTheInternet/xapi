package xapi.fu;

import xapi.fu.api.DoNotOverride;
import xapi.fu.api.Generate;

/**
 * A marker interface we apply to immutable types.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Do extends AutoCloseable {

  Do NOTHING = new Do() {
    @Override
    public final void done() {
    }

    @Override
    public final Do doBefore(Do d) {
      return d;
    }

    @Override
    public final Do doAfter(Do d) {
      return d;
    }
  };

    void done();

    default DoUnsafe unsafe() {
      return this::done;
    }

  @DoNotOverride
  default Runnable toRunnable() {
    return this::done;
  }

  default Do doBefore(Do d) {
      if (d == NOTHING) {
        return this;
      }
    return ()->{
      d.done();
      done();
    };
  }

  @Override
  @DoNotOverride("override done() instead")
  default void close() { // erases exceptions
    done();
  }

  default AutoCloseable closeable() {
    return this;
  }

  default Do doAfter(
      @Generate({
          // n-ary is used to expand a type parameter;
          // we will insert automatically generated methods
          // that extend input arguments by n dimensions.
          // Example, n = 2
          // (Do) -> <I1>(In1<I1>, I1), <I1, I2>(In2<I1, I2>, I1, I2)
          "<n-ary" +
          " n=3",
          // TODO: have meaningful, concise configuration.
          " public=true",
          " final=true",
          " />",
      })
      Do d) {
      if (d == NOTHING) {
        return this;
      }
    return ()->{
      done();
      d.done();
    };
  }

  default Do once() {
      Do[] target = {this};
      return ()->{
        // re-entrance sucks, avoid it
        final Do run;
        synchronized (target) {
          run = target[0];
          if (run == Do.NOTHING) {
            return;
          }
          target[0] = ()->{
            // interim target that forces racers to wait on whoever is actually doing the work.
            Do check;
            int delay = 10;
            for(;;) {
              synchronized (target) {
                check = target[0];
              }
              if (check == Do.NOTHING) {
                return;
              }
              try {
                synchronized (target) {
                  target.wait(++delay);
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.firstLog(this)
                    .log(Do.class, "Interrupted waiting on ", Do.this);
                throw Rethrowable.firstRethrowable(this).rethrow(e);
              }
            }
          };
        }
        // actually do the work, exactly once. First one in starts the work,
        // all others who made it here will wait.
        run.done();
        // all done, let the busy-waiters go...
        synchronized (target) {
          target[0] = Do.NOTHING;
          target.notifyAll();
        }
      };
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

  default Do doIf(Out1<Boolean> filter) {
      Do me = this;
      return ()->{
        if (filter.out1()) {
          me.done();
        }
      };
  }

  default <I> In1<I> ignores1() {
    return ignored->done();
  }
  default <I1, I2> In2<I1, I2> ignores2() {
    return (ig, nored)->done();
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

  static Do of(Runnable r) {
    return r::run;
  }

  static <I1> Do of(In1<I1> of, I1 i1) {
    return of.provide(i1);
  }

  static Do ofUnsafe(Runnable r) {
    return r::run;
  }

  interface DoUnsafe extends Do, Rethrowable{
    void doneUnsafe() throws Throwable;

    @Override
    default DoUnsafe unsafe() {
      return this;
    }

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

  static Do unsafe(DoUnsafe o) {
    return o;
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

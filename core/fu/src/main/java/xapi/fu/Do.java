package xapi.fu;

import xapi.fu.api.DoNotOverride;
import xapi.fu.api.Generate;
import xapi.fu.log.Log;

/**
 * A composable Runnable interface.
 *
 * @author James X. Nelson (james@wetheinter.net)
 * Created on 07/11/15.
 */
@FunctionalInterface
public interface Do extends AutoCloseable {

    Do NOTHING = new Do() {
        @Override
        public final void done() {
        }

        @Override
        public final Do doBefore(Do d) {
            return d == null ? this : d;
        }

        @Override
        public final Do doAfter(Do d) {
            return d == null ? this : d;
        }
    };

    In1<Do> INVOKE = Do::done;

    Out1<Do> NOTHING_FACTORY = Immutable.immutable1(NOTHING);

    void done();

    default DoUnsafe unsafe() {
        return this::done;
    }

    @DoNotOverride
    default Runnable toRunnable() {
        return this::done;
    }

    default Do doBefore(Do d) {
        if (d == NOTHING || d == this || d == null) {
            return this;
        }
        return () -> {
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
            Do d
    ) {
        if (d == NOTHING || d == this || d == null) {
            return this;
        }
        return () -> {
            done();
            d.done();
        };
    }

    default Do once() {
        // re-entrance sucks, we avoid it by efficiently contending on a lock (the "array" below)
        Do[] pointer = {this};
        Mutable<Do> p = Mutable.mutable(Do.class);
        return () -> {
            // taking locks sucks, avoid it
            Do run = pointer[0];
            if (run == Do.NOTHING) {
                // job completed by someone else
                return;
            }
            // doing work inside a lock is bad, so just check / set the target array
            synchronized (pointer) {
                run = pointer[0];
                if (run == Do.NOTHING) {
                    // already done
                    return;
                }
                // reset the target to a blocker callback
                pointer[0] = () -> {
                    // interim target that forces racers to wait on whoever is actually doing the work.
                    // The code below is only ever run in the event of a race condition (or a long running task).
                    Do check;
                    // first, in the event of a race condition on a short task, spin, by default, 100 times...
                    int delay = Integer.parseInt(System.getProperty("xapi.do.spins", "100"));
                    while (delay-- > 0) {
                        // acquire monitor / resync running thread's memory
                        synchronized (pointer) {
                            if (pointer[0] == Do.NOTHING) {
                                return;
                            }
                        }
                    }
                    // ok, it's not a short task.  start waiting.
                    delay = 50;
                    for (; ; ) {
                        synchronized (pointer) {
                            check = pointer[0];
                        }
                        if (check == Do.NOTHING) {
                            return;
                        }
                        try {
                            synchronized (pointer) {
                                pointer.wait(++delay);
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
            // all others who made it here will get a blocker task which spins, then waits.
            run.done();
            // all done.  let anyone waiting go
            synchronized (pointer) {
                // allow anyone who arrives in the future to no-op return w/out acquiring locks
                pointer[0] = Do.NOTHING;
                // wake up any busy-waiters who arrived while we were working go...
                pointer.notifyAll();
            } // anyone spinning will see the updated pointer[] soon
        };
    }

    default <I> In1<I> requireBefore(In1<I> in1) {
        return i -> {
            in1.in(i);
            done();
        };
    }

    default <I> In1<I> requireAfter(In1<I> in1) {
        return i -> {
            done();
            in1.in(i);
        };
    }

    default Do doIf(Out1<Boolean> filter) {
        Do me = this;
        return () -> {
            if (filter.out1()) {
                me.done();
            }
        };
    }

    default <I> In1<I> ignores1() {
        return ignored -> done();
    }

    default <I1, I2> In2<I1, I2> ignores2() {
        return (ig, nored) -> done();
    }

    default <O> Out1<O> returns1(O val) {
        return () -> {
            done();
            return val;
        };
    }

    default <O> Out1<O> returns1Deferred(Out1<O> val) {
        return () -> {
            done();
            return val.out1();
        };
    }

    default <O> Out1<O> returns1Immediate(Out1<O> val) {
        O value = val.out1();
        return () -> {
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

    interface DoUnsafe extends Do, Rethrowable {
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

    /**
     * Semantically equivalent to {@link #once()}, but much less code,
     * at the expense of always checking a lock every time the returned callback is invoked.
     * <p>
     * This probably isn't a big deal, and will be a better choice for GWT-compatible code
     * (who has no reason to pay for the extra code in #once).
     *
     * @return
     */
    default Do onlyOnce() {
        Mutable<Do> once = new Mutable<>(this);
        return In2.in2(once::useThenSet).provide1(Do.INVOKE).provide(Do.NOTHING);
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
    public final DoOnce onlyOnce() {
        return this; // no need to double-wrap
    }

    @Override
    public void done() {
        todo.useThenSet(Do.INVOKE, Do.NOTHING);
    }
}

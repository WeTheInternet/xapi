package xapi.process.api;

import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo.Many;
import xapi.fu.Do;
import xapi.fu.In3Out1;
import xapi.fu.In3Out1.In3Out1Unsafe;
import xapi.fu.data.MapLike;
import xapi.io.api.HasLiveness;
import xapi.process.impl.ConcurrencyServiceAbstract.TimeoutEntry;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.time.impl.ImmutableMoment;
import xapi.util.api.ErrorHandler;
import xapi.util.api.HasLifecycle;
import xapi.util.api.RemovalHandler;

import java.util.concurrent.TimeUnit;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/23/18 @ 3:14 AM.
 */
public class InterruptManager {

    private final Thread interrupter;
    private final Many<Moment, Do> tasks;

    public InterruptManager(Thread interrupter, Many<Moment, Do> tasks) {
        this.interrupter = interrupter;
        this.tasks = tasks;
    }

    public TimeoutEntry requestInterruption(Thread thread, Long time, TimeUnit unit) {
        double millisToWait = unit.toNanos(time) / 1_000_000.;
        Moment deadline = new ImmutableMoment(X_Time.nowPlus(millisToWait));
        final IntTo<Do> forMoment = tasks.get(deadline);
        final Do task = thread::interrupt;
        int index = forMoment.size();
        forMoment.add(task);
        synchronized (tasks) {
            tasks.notifyAll();
        }
        final Do undo = forMoment.removeLater(
            index,
            task
        );
        final TimeoutEntry entry = new TimeoutEntry(undo, interrupter);
        return entry;
    }

    public void shutdown() {
        if (interrupter != null) {
            interrupter.interrupt();
        }
        tasks.removeAll((m, t)->{
            if (t instanceof HasLifecycle) {
                ((HasLifecycle) t).onDestroy();
            }
            if (t instanceof RemovalHandler) {
                ((RemovalHandler) t).remove();
            }
        });
    }
}

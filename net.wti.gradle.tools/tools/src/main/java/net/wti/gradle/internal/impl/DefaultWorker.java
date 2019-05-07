package net.wti.gradle.internal.impl;

import net.wti.gradle.internal.api.HasWork;
import org.gradle.api.Action;
import org.gradle.internal.Actions;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static net.wti.gradle.internal.api.ReadyState.RUN_FINALLY;

/**
 * A generic work-beast for executing integer-ordered tasks.
 *
 * While we use a concurrent NavigableMap for callbacks,
 * we currently expect to run in serial during gradle's configuration phase.
 *
 * We will likely extract this into a xapi-fu form later,
 * once we get a better handle on generalized use-cases.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2/9/19 @ 3:50 AM.
 */
public class DefaultWorker implements HasWork {

    private final ConcurrentNavigableMap<Integer, StartCallback> callbacks;

    public DefaultWorker() {
        // we prefer a skip list map, since we use .firstKey() a lot, and that's much faster
        // in the skip-list-map's linked list than it is in, say, a red-black tree (TreeMap).
        callbacks = new ConcurrentSkipListMap<>();
    }

    @Override
    public boolean hasWork() {
        return !callbacks.isEmpty();
    }

    @Override
    public boolean hasWork(int upTo) {
        // hm. memoize this.
        return !callbacks.isEmpty() && callbacks.firstKey() <= upTo;
    }

    @Override
    public Entry<Integer, HasWork> findWork(int upTo) {
        Entry<Integer, HasWork> candidate = null;
        if (!callbacks.isEmpty()) {
            final Entry<Integer, StartCallback> check = callbacks.firstEntry();
            if (check.getKey() <= upTo) {
                candidate = new SimpleImmutableEntry<>(check.getKey(), this);
            }
        }
        return candidate;
    }

    /**
     * Run all registered project-scoped callbacks, upTo a given priority.
     **
     * @return true if we did any work.
     *
     * This can allow graph nodes who wish to "iterate until all children are done",
     * in case children are adding tasks to each other.
     */
    @Override
    public boolean drainTasks(int upTo) {
        boolean hasWork = false;
        Entry<Integer, HasWork> work;
        while ((work = findWork(upTo)) != null) {
            hasWork = true;
            work.getValue().doWork();
        }
        return hasWork;
    }

    @Override
    public void doWork() {
        final Entry<Integer, StartCallback> entry = callbacks.pollFirstEntry();
        if (entry != null) {
            final StartCallback callback = entry.getValue();
            callback.execute(entry.getKey());
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public boolean whenReady(int priority, Action<Integer> newItem) {
        return callbacks.compute(priority, (key, oldItem) ->
            oldItem == null ?
                firstCallback(priority, newItem) :
                oldItem.append(newItem)
        ).first;
    }

    protected StartCallback firstCallback(int priority, Action<Integer> newItem) {
        return new StartCallback(priority, newItem);
    }

    protected class StartCallback implements Action<Integer> {

        private final Action<Integer> delegate;
        private final int priority;
        private final boolean first;
        private volatile StartCallback after;

        public StartCallback(int priority, Action<Integer> delegate) {
            this.delegate = Actions.composite(this::firstRun, delegate);
            this.priority = priority;
            first = true;
        }

        public StartCallback(StartCallback before, Action<Integer> after) {
            this.delegate = Actions.composite(before, after);
            this.priority = before.priority;
            before.after = this;
            first = false;
        }

        protected void firstRun(Integer priority) {
            callbacks.computeIfPresent(priority, (key, cb)->
                cb == StartCallback.this ? null : cb
            );
        }
        protected void lastRun(Integer priority) {

        }

        @Override
        public void execute(Integer priority) {
            delegate.execute(priority);
            if (this.priority < RUN_FINALLY) {
                // Any task before RUN_FINALLY will always run anything lower priority that itself, when it's the next queued item (no batching)
                if (this.priority > Integer.MIN_VALUE) {
                    // Any task at Integer.MIN_VALUE will be getting drained in a loop, so no need to do here (and get an overflow...)
                    drainTasks(this.priority - 1);
                }
            } else if (this.priority != RUN_FINALLY){
                // Any task that's after RUN_FINALLY will clear all RUN_FINALLY between each item in a batch
                drainTasks(RUN_FINALLY);
            }
            // Note, Any task *at* RUN_FINALLY will be getting drained in a loop, so no need to recurse here, between items.

            if (after == null) {
                // let any subclasses know we are the last run
                lastRun(priority);
            }
        }

        protected StartCallback append(Action<Integer> newItem) {
            return new StartCallback(this, newItem);
        }
    }

}

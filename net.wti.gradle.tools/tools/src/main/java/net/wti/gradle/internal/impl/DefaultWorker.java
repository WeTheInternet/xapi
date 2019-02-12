package net.wti.gradle.internal.impl;

import net.wti.gradle.internal.api.HasWork;
import org.gradle.api.Action;
import org.gradle.internal.Actions;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

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

    private final NavigableMap<Integer, Action<Integer>> callbacks;

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
            final Entry<Integer, Action<Integer>> check = callbacks.firstEntry();
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
        boolean hasWork = hasWork(upTo);
        if (!hasWork) {
            return false;
        }
        Entry<Integer, HasWork> work;
        while ((work = findWork(upTo)) != null) {
            work.getValue().doWork();
        }
        return true;
    }

    @Override
    public void doWork() {
        final Entry<Integer, Action<Integer>> entry;
        entry = callbacks.pollFirstEntry();
        if (entry == null) {
            return;
        }
        final Action<Integer> callback = entry.getValue();
        callback.execute(entry.getKey());
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public boolean whenReady(int priority, Action<Integer> newItem) {
        final Action<Integer> result = callbacks.compute(priority, (key, oldItem) ->
            // TODO: use a multimap instead of compositing actions together,
            //  so we can honor prioritized callback ordering
            oldItem == null ? newItem : Actions.composite(oldItem, newItem)
        );
        return result == newItem;
    }

}

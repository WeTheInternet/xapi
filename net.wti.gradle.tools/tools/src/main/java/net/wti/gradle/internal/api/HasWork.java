package net.wti.gradle.internal.api;

import org.gradle.api.Action;

import java.util.Map.Entry;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2/1/19 @ 2:37 AM.
 */
public interface HasWork {

    default boolean hasWork() {
        return hasWork(Integer.MAX_VALUE);
    }

    default boolean hasWork(int upTo) {
        final Entry<Integer, HasWork> found = findWork(upTo);
        return found != null;
    }

    Entry<Integer, HasWork> findWork(int upTo);

    /**
     * Add a prioritized task to a work queue.
     *
     * Tasks are drained from lowest number to highest.
     * Be sure to read the documentation of {@link ReadyState}.
     *
     * If you add to an earlier priority when flushing a later one,
     * the newly added callback will be the next item pulled off the work queue.
     *
     * @param priority When to run this action.
     *                 All xapi code uses shorts from {@link ReadyState}.
     *                 Users will want to derive their own constants from these values.
     * @param callback The action to execute when the given ready state is being flushed.
     *                 The callback's parameter is the integer priority that was specified
     *                 (so you can reuse a single Action object for more than one specific value).
     * @return true if this was the first item added at the given priority.  See overrides of this method.
     */
    boolean whenReady(int priority, Action<Integer> callback);

    boolean drainTasks(int upTo);

    void doWork();

    default void bindLifecycle(HasWork child) {
        assert child != this;
        final Action<Integer> drain = this::drainTasks;
        // TODO: instead of this abomination of maybe-not-even-used callbacks,
        // we instead move this to something with .getParent(),
        // so we can simply "ask parent to call-us on-demand",
        // which will eliminate the hideous depth-first search we are doing now...
        for (int lifecycle : lifecycles()) {
            whenReady(lifecycle, drain);
        }
    }

    default int[] lifecycles() {
        return ReadyState.all();
    }
}

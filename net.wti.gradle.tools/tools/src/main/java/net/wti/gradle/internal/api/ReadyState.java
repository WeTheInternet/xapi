package net.wti.gradle.internal.api;

/**
 * A set of short integers between -0x6000 and +0x6000 representing a "readiness state".
 *
 * The values are laid out to have plenty of bitmask space between each item.
 *
 * There are three lifecycles, CREATED, READY and FINISHED.
 * Each lifecycle has before, "during", and after int space.
 *
 * These are the numbers used by xapi systems,
 * so you can prioritize your items between our lifecycle,
 * with some partitioning space left to order your own items relative to each other.
 *
 * We use short in method signatures to encourage you to create constants (with documentation).
 *
 * Also, using short leaves room to create composite states within a single int or long,
 * in case we want to do something like:
 * "whenConfigurationReady(0x123L, ready->{})" // saves to 0x123
 * and "whenTaskReady(0x123L, ready->{})", // saves to 0x8123 (add Short.MAX_VALUE+1, 0x8000)
 * where we can flush all of the whenConfigurationReady callbacks,
 * without touching the intspace (rather than shortspace) of the whenTaskReady callbacks;
 * the task callbacks just add 0x8000,
 * and we simply don't go beyond Short.MAX_VALUE during the configuration phase
 * (using a NavigableMap for now, ConcurrentSkipListMap;
 * perhaps better to simply use two different maps... or maybe with just two heaps).
 *
 * so, flushUpTo(0x7FFF) would cover configuration,
 * and flushUpTo(0x7FFF_FFFF) would cover execution.
 *
 * NOTE THAT, FOR NOW, -0x8000_0000 is the minimum boundary in all phases.
 *
 * "Run finally phase" commands occupy -0x8000_0000 to -0x8001 (Integer.MIN_VALUE to Short.MIN_VALUE - 1)
 * This space is reserved for "run as soon as calling code finishes and queue is queried",
 * rather than scheduling future operations.
 *
 * "Configuration phase" is from -0x8000 to -0x7FFF (Short.MIN_VALUE to Short.MAX_VALUE)
 * The standard set of callbacks to drain when work is completed.
 *
 * "Execution phase" values range from 0x8000 to 0x7FFF_FFFF. (Short.MAX_VALUE + 1 to Integer.MAX_VALUE)
 * Tasks scheduled for execution only during the execution phase.
 * Allows the configuration phase to orchestrate series of operations to perform only at task execution time.
 * Storing such tasks on a leaf node can enable us to lazily avoid doing unneeded work.
 *
 *
 * This setup precludes execution phase from having pre-scheduled BEFORE_CREATED to BEFORE_READY bitspace,
 * but it's kind of silly to be at a "before ready" state during execution anyway.
 *
 * When thinking about execution-time callback scheduling, only consider positive integer space.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2/1/19 @ 2:55 AM.
 */
public interface ReadyState {

    int RUN_FINALLY = Short.MIN_VALUE - 1;
    short BEFORE_CREATED = -0x6000;
    short CREATED = -0x4000;
    short AFTER_CREATED = -0x2000;
    short BEFORE_READY = -0x800;
    short READY = 0;
    short AFTER_READY = 0x800;
    short BEFORE_FINISHED = 0x2000;
    short FINISHED = 0x4000;
    short AFTER_FINISHED = 0x6000;
    int EXECUTE = Short.MAX_VALUE + 1;

    static int[] all() {
        return new int[]{
            RUN_FINALLY,
            BEFORE_CREATED, CREATED, AFTER_CREATED,
            BEFORE_READY, READY, AFTER_READY,
            BEFORE_FINISHED, FINISHED, AFTER_FINISHED,
            EXECUTE
        };
    }
}

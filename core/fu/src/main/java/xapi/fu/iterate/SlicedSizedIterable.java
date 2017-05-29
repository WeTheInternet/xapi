package xapi.fu.iterate;

import xapi.fu.Immutable;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;

import java.util.NoSuchElementException;

import static xapi.fu.Immutable.immutable1;

/**
 * A {@link SizedIterable} comprised of two other sized iterables,
 * with the first iterable sliced at a given index;
 * that is, we will iterate through part of the first iterator,
 * then switch to the second iterator until exhausted,
 * then switch back to the first iterator, etc.
 *
 * We accept a drain parameter to decide whether to only take
 * the given number of items from a source then discarding the rest.
 *
 * Returning a negative number will cause that particular source to
 * be drained as soon as its turn to be the source comes up.
 *
 * When running in drain mode, each iterable will be drained until
 * their given limit is reached, and if both limits should return 0,
 * we will exhaust the before iterator, followed by the after.
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 5/27/17.
 */
public class SlicedSizedIterable<T> implements SizedIterable<T> {

    public static final int DRAIN_ALL = -1;
    private final SizedIterable<T> before, after;
    private final Out1<Integer> limitBefore;
    private final Out1<Integer> limitAfter;
    private final boolean drain;

    public SlicedSizedIterable(int fromBefore, SizedIterable<T> before, int fromAfter, SizedIterable<T> after) {
        this(
            immutable1(fromBefore), before,
            immutable1(fromAfter), after,
            false
        );
    }

    public SlicedSizedIterable(int fromBefore, SizedIterable<T> before, int fromAfter, SizedIterable<T> after, boolean drain) {
        this(
            immutable1(fromBefore), before,
            immutable1(fromAfter), after,
            drain
        );
    }

    /**
     * Used to interleave two iterators by round-robining a set number
     * of items from each.
     *
     * The first pair of limit + iterable will expose N items, if available,
     * from the before iterable, then M items, if available, from the after iterable.
     *
     * N is calculated from limitBefore factory,
     * M from the limitAfter factory.
     *
     * If either factory returns a negative number,
     * it will completely exhaust the source iterator.
     *
     * Return 0 to skip a turn (if you return immutable 0,
     * then you are effectively ignoring that source... and paying
     * way too much for this class).
     *
     * If the boolean drain is set to true, we will keep pulling
     * items off both iterators until they are emptied,
     * even if the limits both return 0, we will just exhaust the
     * first iterator, then the last one.
     *
     * Be aware that if you try to do any fancy state management
     * on a non-draining iterator, where you expect the limits
     * to be called back and forth, that we will also call those
     * limits when computing size, so be careful that such cases
     * correctly check their state before assuming we have consumed the limit,
     * or if calling code was just peeking on the size.
     *
     * @param limitBefore
     * @param before
     * @param limitAfter
     * @param after
     * @param drain
     */
    public SlicedSizedIterable(Out1<Integer> limitBefore,
                               SizedIterable<T> before,
                               Out1<Integer> limitAfter,
                               SizedIterable<T> after,
                               boolean drain) {
        this.before = before == null ? EmptyIterator.none() : before;
        this.after = after == null ? EmptyIterator.none() : after;
        this.limitBefore = limitBefore;
        this.limitAfter = limitAfter;
        this.drain = drain;
    }

    @Override
    public int size() {
        if (drain) {
            return before.size() + after.size();
        } else {
            final Integer allowedBefore = limitBefore.out1();
            final Integer allowedAfter = limitAfter.out1();
            return allowedBefore < 0 ? before.size() : allowedBefore
             + allowedAfter < 0 ? after.size() : allowedAfter;
        }
    }

    private final class SlicedSizedIterator implements SizedIterator<T> {

        // we are trusting that supplied iterators are threadsafe
        // by putting the lock here, in this anonymous class.
        // Had we put the lock in the SlicedSizedIterable root class,
        // we could be inviting deadlock if two external callsites
        // each unknowingly get the same SlicedSizedIterable,
        // then try to ...iterate through each other, at the same time.
        // (for example, through shared pointers to empty iterator)
        private final Object lock = new Object();

        private final SizedIterator<T> useFirst = before.iterator();
        private final SizedIterator<T> useLast = after.iterator();

        int allowedBefore = limitBefore.out1();
        int allowedAfter = limitAfter.out1();

        @Override
        public boolean hasNext() {
            synchronized (lock) {
                if (drain) {
                    return useFirst.hasNext() || useLast.hasNext();
                }
                if (allowedBefore < 0 && useFirst.hasNext()) {
                    return true;
                }
                if (allowedAfter < 0 && useLast.hasNext()) {
                    return true;
                }
                return allowedBefore > 0 || allowedAfter > 0;
            }
        }

        @Override
        public T next() {
            synchronized (lock) {
                if (allowedBefore != 0) {
                    // allow -1 to mean "use all of before"
                    allowedBefore --;
                    if (useFirst.hasNext()) {
                        return useFirst.next();
                    }
                }

                if (allowedAfter != 0) {
                    allowedAfter --;
                    if (useLast.hasNext()) {
                        return useLast.next();
                    }
                }
                if (!drain) {
                    throw new NoSuchElementException("Sliced iterator not allowed to drain sources");
                }
                if (useFirst.hasNext()) {
                    allowedBefore = limitBefore.out1();
                    allowedBefore --;
                    return useFirst.next();
                }
                if (useLast.hasNext()) {
                    allowedAfter = limitAfter.out1();
                    allowedAfter --;
                    return useLast.next();
                }
                throw new NoSuchElementException("Tried to use drained iterators");
            }
        }

        @Override
        public int size() {
            synchronized (lock) {
                if (drain) {
                    return useFirst.size() + useLast.size();
                }
                return
                    (allowedBefore < 0 ? useFirst.size() : allowedBefore)
                    + (allowedAfter < 0 ? useLast.size() : allowedAfter);
            }
        }

        @Override
        public String toString() {
            return MappedIterable.mapped(SlicedSizedIterator::new)
                .join("[",", ", "]");
        }
    }

    @Override
    public SizedIterator<T> iterator() {
        return new SlicedSizedIterator();
    }
}

package xapi.fu.iterate;

/**
 * A {@link SizedIterable} comprised of two other sized iterables.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 5/27/17.
 */
public class BiSizedIterable<T> implements SizedIterable<T> {

    private final SizedIterable<T> before, after;

    public BiSizedIterable(SizedIterable<T> before, SizedIterable<T> after) {
        this.before = before == null ? EmptyIterator.none() : before;
        this.after = after == null ? EmptyIterator.none() : after;
    }

    @Override
    public int size() {
        return before.size() + after.size();
    }

    private final class BiSizedIterator implements SizedIterator<T> {

        // we are trusting that supplied iterators are threadsafe
        // by putting the lock here, in this anonymous class.
        // Had we put the lock in the BiSizedIterable root class,
        // we could be inviting deadlock if two external callsites
        // each unknowingly get the same SizedIterable,
        // then try to ...iterate through each other, at the same time.
        // (for example, through shared pointers to empty iterator)
        private final Object lock = new Object();

        private SizedIterator<T> useFirst = before.iterator();
        final SizedIterator<T> useLast = after.iterator();

        @Override
        public boolean hasNext() {
            synchronized (lock) {
                if (useFirst != null) {
                    if (useFirst.hasNext()) {
                        return true;
                    }
                    useFirst = null;
                }
                return useLast.hasNext();
            }
        }

        @Override
        public T next() {
            synchronized (lock) {
                if (useFirst != null) {
                    final T ret = useFirst.next();
                    if (!useFirst.hasNext()) {
                        // eagerly null out, in case
                        // calling code was racing after .hasNext()
                        // ...it would have failed later anyway,
                        // but we'd like to have a chance to try
                        // the next iterator before failing
                        useFirst = null;
                    }
                    return ret;
                }
                return useLast.next();
            }
        }

        @Override
        public int size() {
            synchronized (lock) {
                if (useFirst == null) {
                    return useLast.size();
                }
                return useFirst.size() + useLast.size();
            }
        }

    }

    @Override
    public SizedIterator<T> iterator() {
        return new BiSizedIterator();
    }
}

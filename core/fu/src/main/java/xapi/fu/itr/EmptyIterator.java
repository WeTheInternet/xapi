package xapi.fu.itr;

import java.util.NoSuchElementException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
public final class EmptyIterator <I> implements SizedIterator <I> {

    @SuppressWarnings("unchecked")
    public static final SizedIterator EMPTY = new EmptyIterator();
    public static final SizedIterable NONE = new SizedIterable() {
        @Override
        public SizedIterator iterator() {
            return EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public SizedIterable plus(SizedIterable more) {
            return more == null ? this : more; // empty will erase itself,
            // this allows you to assign a SizedIterable to none(), and then just .plus() it.
            // the empty item will always try to erase itself...
        }
    };

    public static <I> SizedIterable<I> none() {
        return NONE;
    }

    public static <I> SizedIterator<I> empty() {
        return EMPTY;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public I next() {
        throw new NoSuchElementException("empty");
    }

    @Override
    public int size() {
        return 0;
    }
}

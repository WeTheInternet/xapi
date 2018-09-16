package xapi.fu.iterate;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.In2;

/**
 * A class to collect up general iterable-related utilities.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 12:59 AM.
 */
public class X_Itr {

    private static final In2<Iterable<Object>, In1<Object>> FOR_ALL = X_Itr::invokeAll;

    public static <T> void invokeAll(Iterable<T> source, In1<T> callback) {
        // Subtypes might expose an optimized forEach, so we'll pay a little more in lambda overhead to use forEach...
        source.forEach(callback.toConsumer());
    }

    /**
     * Return a bi-input which accepts an iterable of items, and a callback of said items.
     *
     * Useful for cases like:
     * {@code Maybe<Iterable<Do>> s;
     * s.readIfPresent(In1.from2(forAllAdapter(), Do.INVOKE)); }
     */
    @SuppressWarnings("unchecked") // The type signature will have to be good enough.
    public static <T> In2<Iterable<T>, In1<T>> forAllAdapter() {
        // we don't want to make the field raw, as we will return an erased object, which won't work.
        // so, we defer the cheater task 'til here.  Dirty, but better than creating 6,000,000 lambda classes.
        return (In2)FOR_ALL;
    }

    /**
     * Return an input which accepts an iterable of Do objects, and invokes them all.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Iterable<Do>> In1<T> invokeAdapter() {
        return In1.from2(forAllAdapter(), Do.INVOKE);
    }
}

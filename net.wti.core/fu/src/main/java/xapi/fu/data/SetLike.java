package xapi.fu.data;

import xapi.fu.api.Ignore;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
@Ignore("model") // a model that implements SetLike will be implementing all methods itself; ignore anything defined in this type.
public interface SetLike <V> extends CollectionLike<V> {

    default SetLike<V> add(V value) {
        addAndReturn(value);
        return this;
    }

    default boolean addIfMissing(V value) {
        return addAndReturn(value) != null;
    }

    default SetLike<V> addNow(Iterable<? extends V> items) {
        CollectionLike.super.addNow(items);
        return this;
    }

    default boolean contains(V value) {
        for (V has : this) {
            if (value == null ? has == null : value.equals(has)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add the value to your set,
     * and only return said value if you wrote anything.
     *
     * If you already contains() the argument, return null.
     *
     * Instead of "return true on add" semantics, this does
     * "return useful value only if there was a state change".
     *
     * Note that, with standard hashCode/equals/compareTo semantics,
     * it is possible to return a different value than what you sent
     * (and this can be important information you can use to absorb state, etc).
     *
     * @param value The value to add
     * @return The value you removed from underlying set/map.
     *
     * Do NOT prefer returning the argument sent to you,
     * unless it really is impossible for you to read back your own discarded value.
     */
    V addAndReturn(V value);

    default boolean remove(V value) {
        return removeAndReturn(value) != null;
    }

    default boolean removeAll(V value) {
        boolean removed = false;
        while(remove(value)) {
            removed = true;
        }
        return removed;
    }

    /**
     * Remote the value from your set,
     * and only return the removed value.
     *
     * Return null to signal "no value removed".
     *
     * @param value The value to remove
     * @return The reference of the removed value from underlying collection
     *         i.e. return the value of Map.remove(keyFor(value));
     */
    V removeAndReturn(V value);

}

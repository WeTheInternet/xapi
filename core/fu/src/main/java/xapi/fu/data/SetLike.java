package xapi.fu.data;

import xapi.fu.MappedIterable;
import xapi.fu.api.Clearable;
import xapi.fu.has.HasItems;
import xapi.fu.iterate.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public interface SetLike <V> extends SizedIterable<V>, Clearable, HasItems<V> {

    default boolean add(V value) {
        return addAndReturn(value) != null;
    }

    default boolean contains(V value) {
        for (V has : this) {
            if (value == null ? has == null : value.equals(has)) {
                return true;
            }
        }
        return false;
    }

    V addAndReturn(V value);

    default boolean remove(V value) {
        return removeAndReturn(value) != null;
    }

    V removeAndReturn(V value);

    @Override
    default MappedIterable<V> forEachItem() {
        return this;
    }
}

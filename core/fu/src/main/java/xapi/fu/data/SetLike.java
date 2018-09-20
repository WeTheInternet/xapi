package xapi.fu.data;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public interface SetLike <V> extends CollectionLike<V> {

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

}

package xapi.fu.data;

import xapi.fu.Out2;
import xapi.fu.X_Fu;
import xapi.fu.api.Ignore;
import xapi.fu.itr.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
@Ignore("model") // a model that implements ListLike will be implementing all methods itself; ignore anything defined in this type.
public interface ListLike <V> extends CollectionLike<V> {

    V get(int pos);
    V set(int pos, V value);
    V remove(int pos);
    default int find(V value, boolean identity) {
        return find(value, 0, identity);
    }

    default int find(V value, int start, boolean identity) {
        int i = start, s = size();
        if (identity) {
            while(i<s) {
                if (get(i) == value) {
                    return i;
                }
                i++;
            }
            return -1;
        } else {
            while(i<s) {
                if (X_Fu.equal(get(i), value)) {
                    return i;
                }
                i++;
            }
            return -1;
        }
    }

    default int removeFirst(V value, boolean identity) {
        int ind = find(value, identity);
        if (ind != -1) {
            remove(ind);
        }
        return ind;
    }

    default boolean removeAll(V value, boolean identity) {
        int ind = 0;
        boolean removed = false;
        while ( (ind = find(value, ind, identity)) != -1) {
            remove(ind);
            removed = true;
        }
        return removed;
    }

    default ListLike<V> add(V value) {
        set(size(), value);
        return this;
    }

    default SizedIterable<Out2<Integer, V>> valuesIndexed() {
        int[] pos = {0};
        return SizedIterable.of(this, v->Out2.out2Immutable(pos[0]++, v) );
    }

    default ListLike<V> addNow(Iterable<? extends V> items) {
        CollectionLike.super.addNow(items);
        return this;
    }
}

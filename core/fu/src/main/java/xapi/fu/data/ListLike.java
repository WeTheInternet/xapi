package xapi.fu.data;

import xapi.fu.Out2;
import xapi.fu.itr.SizedIterable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public interface ListLike <V> extends CollectionLike<V> {

    V get(int pos);
    V set(int pos, V value);
    V remove(int pos);

    default ListLike<V> add(V value) {
        set(size(), value);
        return this;
    }

    default SizedIterable<Out2<Integer, V>> valuesIndexed() {
        int[] pos = {0};
        return SizedIterable.of(this, v->Out2.out2Immutable(pos[0]++, v) );
    }

}
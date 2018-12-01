package xapi.fu.java;

import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.fu.data.ListLike;
import xapi.fu.data.MultiList;
import xapi.fu.has.HasLock;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/25/18 @ 7:11 AM.
 */
public class JdkMultiList <K, V> extends MapAdapter<K, ListLike<V>> implements MultiList<K, V> {

    private final In1Out1<K, ListLike<V>> listFactory;

    public JdkMultiList() {
        this(X_Jdk.defaultMap());
    }

    public JdkMultiList(Map<K, ListLike<V>> map) {
        this(map, map instanceof ConcurrentMap ? X_Jdk::listArrayConcurrent : X_Jdk::listArray);
    }

    public JdkMultiList(Out1<ListLike<V>> listFactory) {
        this(listFactory.ignoreIn1());
    }

    public JdkMultiList(Map<K, ListLike<V>> map, Out1<ListLike<V>> listFactory) {
        this(map, listFactory.ignoreIn1());
    }

    public JdkMultiList(In1Out1<K, ListLike<V>> listFactory) {
        super();
        this.listFactory = listFactory;
    }

    public JdkMultiList(Map<K, ListLike<V>> map, In1Out1<K, ListLike<V>> listFactory) {
        super(map);
        this.listFactory = listFactory;
    }

    @Override
    public ListLike<V> get(K key) {
        return HasLock.alwaysLock(this, ()->{
            if (has(key)) {
                return super.get(key);
            }
            final ListLike<V> val = createList(key);
            put(key, val);
            return val;
        });
    }

    @Override
    public ListLike<V> createList(K key) {
        return listFactory.io(key);
    }
}

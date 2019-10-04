package xapi.fu.java;

import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.fu.data.MultiSet;
import xapi.fu.data.SetLike;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/25/18 @ 7:11 AM.
 */
public class JdkMultiSet<K, V> extends MapAdapter<K, SetLike<V>> implements MultiSet<K, V> {

    private final In1Out1<K, SetLike<V>> listFactory;

    public JdkMultiSet() {
        this(X_Jdk.defaultMap());
    }

    public JdkMultiSet(Map<K, SetLike<V>> map) {
        this(map, pickMapImpl(map));
    }

    private static <K, V> Out1<SetLike<V>> pickMapImpl(Map<K, SetLike<V>> map) {
        return map instanceof ConcurrentMap ?
            map instanceof ConcurrentHashMap ?
                X_Jdk::setHashConcurrent :
                X_Jdk::setLinkedSynchronized :
            map instanceof HashMap ?
                X_Jdk::setHash :
                X_Jdk::setLinked;
    }

    public JdkMultiSet(Out1<SetLike<V>> listFactory) {
        this(listFactory.ignoreIn1());
    }

    public JdkMultiSet(Map<K, SetLike<V>> map, Out1<SetLike<V>> listFactory) {
        this(map, listFactory.ignoreIn1());
    }
    public JdkMultiSet(In1Out1<K, SetLike<V>> listFactory) {
        this.listFactory = listFactory;
    }

    public JdkMultiSet(Map<K, SetLike<V>> map, In1Out1<K, SetLike<V>> listFactory) {
        super(map);
        this.listFactory = listFactory;
    }

    @Override
    public SetLike<V> get(K key) {
        return computeIfAbsent(key, this::createSet);
    }

    @Override
    public SetLike<V> createSet(K key) {
        return listFactory.io(key);
    }

}

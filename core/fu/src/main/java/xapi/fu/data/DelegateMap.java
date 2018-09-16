package xapi.fu.data;

import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.fu.iterate.SizedIterable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 3:38 AM.
 */
public class DelegateMap <K, V> implements MapLike<K, V> {

    private final In2Out1<K, V, V> setter;
    private final In1Out1<K, V> getter;
    private final In1Out1<K, Boolean> hasser;
    private final In1Out1<K, V> remover;
    private final Out1<SizedIterable<K>> keys;
    private final Do clear;
    private final Out1<Integer> sizer;

    public DelegateMap(
        In2Out1<K, V, V> setter,
        In1Out1<K, V> getter,
        In1Out1<K, Boolean> hasser,
        In1Out1<K, V> remover,
        Out1<SizedIterable<K>> keys,
        Do clear,
        Out1<Integer> sizer
    ) {
        this.setter = setter;
        this.getter = getter;
        this.hasser = hasser;
        this.remover = remover;
        this.keys = keys;
        this.clear = clear;
        this.sizer = sizer;
    }

    @Override
    public V put(K key, V value) {
        return setter.io(key, value);
    }

    @Override
    public V get(K key) {
        return getter.io(key);
    }

    @Override
    public boolean has(K key) {
        return hasser.io(key);
    }

    @Override
    public V remove(K key) {
        return remover.io(key);
    }

    @Override
    public SizedIterable<K> keys() {
        return keys.out1();
    }

    @Override
    public void clear() {
        clear.done();
    }

    @Override
    public int size() {
        return sizer.out1();
    }
}

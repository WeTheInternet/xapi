package xapi.fu.data;

import xapi.fu.*;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.SizedIterator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 3:38 AM.
 */
public class DelegateMap <K, V> implements MapLike<K, V> {

    private final In2Out1<K, V, V> setter;
    private final In1Out1<K, V> getter;
    private final In1Out1<K, Boolean> hasser;
    private final In1Out1<K, V> remover;
    private final Out1<SizedIterable<Out2<K, V>>> entries;
    private final Do clear;
    private final Out1<Integer> sizer;

    public DelegateMap(
        In2Out1<K, V, V> setter,
        In1Out1<K, V> getter,
        In1Out1<K, Boolean> hasser,
        In1Out1<K, V> remover,
        Out1<SizedIterable<Out2<K, V>>> entries,
        Do clear,
        Out1<Integer> sizer
    ) {
        this.setter = setter;
        this.getter = getter;
        this.hasser = hasser;
        this.remover = remover;
        this.entries = entries;
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
        return entries.out1().map(Out2::out1);
    }

    @Override
    public void clear() {
        clear.done();
    }

    @Override
    public int size() {
        return sizer.out1();
    }

    @Override
    public SizedIterator<Out2<K, V>> iterator() {
        return entries.out1().iterator();
    }
}

package xapi.fu.data;

import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.fu.io.InAOutA;
import xapi.fu.iterate.SizedIterator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 3:29 AM.
 */
public class DelegateList <V> implements ListLike<V> {

    private final In1Out1<Integer, V> getter;
    private final In2Out1<Integer, V, V> setter;
    private final In1Out1<Integer, V> remover;
    private final Out1<SizedIterator<V>> iterator;
    private final Do clearer;
    private final Out1<Integer> sizer;

    public DelegateList(
        In1Out1<Integer, V> getter,
        In2Out1<Integer, V, V> setter,
        In1Out1<Integer, V> remover,
        Out1<SizedIterator<V>> iterator,
        Do clearer,
        Out1<Integer> sizer
    ) {
        this.getter = getter;
        this.setter = setter;
        this.remover = remover;
        this.iterator = iterator;
        this.clearer = clearer;
        this.sizer = sizer;
    }

    public DelegateList<V> withGetter(InAOutA<In1Out1<Integer, V>> getter) {
        return new DelegateList<>(getter.io(this.getter), setter, remover, iterator, clearer, sizer);
    }

    public DelegateList<V> withSetter(InAOutA<In2Out1<Integer, V, V>> setter) {
        return new DelegateList<>(this.getter, setter.io(this.setter), remover, iterator, clearer, sizer);
    }

    public DelegateList<V> withRemover(InAOutA<In1Out1<Integer, V>> remover) {
        return new DelegateList<>(this.getter, this.setter, remover.io(this.remover), iterator, clearer, sizer);
    }

    public DelegateList<V> withIterator(InAOutA<Out1<SizedIterator<V>>> iterator) {
        return new DelegateList<>(this.getter, this.setter, this.remover, iterator.io(this.iterator), clearer, sizer);
    }

    public DelegateList<V> withClearer(InAOutA<Do> clearer) {
        return new DelegateList<>(this.getter, this.setter, this.remover, this.iterator, clearer.io(this.clearer), sizer);
    }

    public DelegateList<V> withSizer(InAOutA<Out1<Integer>> sizer) {
        return new DelegateList<>(this.getter, this.setter, this.remover, this.iterator, clearer, sizer.io(this.sizer));
    }

    @Override
    public V get(int pos) {
        return getter.io(pos);
    }

    @Override
    public V set(int pos, V value) {
        return setter.io(pos, value);
    }

    @Override
    public V remove(int pos) {
        return remover.io(pos);
    }

    @Override
    public SizedIterator<V> iterator() {
        return iterator.out1();
    }

    @Override
    public void clear() {
        clearer.done();
    }

    @Override
    public int size() {
        return sizer.out1();
    }
}

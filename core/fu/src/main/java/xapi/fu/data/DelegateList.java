package xapi.fu.data;

import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.fu.iterate.SizedIterator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 3:29 AM.
 */
public class DelegateList <T> implements ListLike<T> {

    private final In1Out1<Integer, T> getter;
    private final In2Out1<Integer, T, T> setter;
    private final In1Out1<Integer, T> remover;
    private final Out1<SizedIterator<T>> iterator;
    private final Do clearer;
    private final Out1<Integer> sizer;

    public DelegateList(
        In1Out1<Integer, T> getter,
        In2Out1<Integer, T, T> setter,
        In1Out1<Integer, T> remover,
        Out1<SizedIterator<T>> iterator,
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

    @Override
    public T get(int pos) {
        return getter.io(pos);
    }

    @Override
    public T set(int pos, T value) {
        return setter.io(pos, value);
    }

    @Override
    public T remove(int pos) {
        return remover.io(pos);
    }

    @Override
    public SizedIterator<T> iterator() {
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

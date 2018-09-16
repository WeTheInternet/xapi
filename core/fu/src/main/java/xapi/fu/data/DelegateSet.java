package xapi.fu.data;

import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.fu.iterate.SizedIterator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 3:35 AM.
 */
public class DelegateSet <T> implements SetLike <T> {

    private final In1Out1<T, T> adder;
    private final In1Out1<T, T> remover;
    private final Out1<SizedIterator<T>> iterator;
    private final Do clearer;
    private final Out1<Integer> sizer;

    public DelegateSet(
        In1Out1<T, T> adder,
        In1Out1<T, T> remover,
        Out1<SizedIterator<T>> iterator,
        Do clearer,
        Out1<Integer> sizer
    ) {
        this.adder = adder;
        this.remover = remover;
        this.iterator = iterator;
        this.clearer = clearer;
        this.sizer = sizer;
    }

    @Override
    public T addAndReturn(T value) {
        return adder.io(value);
    }

    @Override
    public T removeAndReturn(T value) {
        return remover.io(value);
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

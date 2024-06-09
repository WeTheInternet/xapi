package xapi.fu.java;

import xapi.fu.Debuggable;
import xapi.fu.Maybe;
import xapi.fu.data.ListLike;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.CachingIterator;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.SizedIterator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/21/17.
 */
public class ListAdapter<T> implements ListLike<T>, Serializable {

    protected List<T> list;
    private boolean sparse;

    public ListAdapter() {
        this(new ArrayList<>());
    }

    public ListAdapter(List<T> list) {
        this.list = list;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public T get(int pos) {
        return list.get(pos);
    }

    @Override
    public T set(int pos, T value) {
        int s = list.size();
        if (pos == s) {
            list.add(value);
        } else if (pos < s) {
            return list.set(s, value);
        } else {
            if (isSparse()) {
                return list.set(s, value);
            } else {
                // here is where it gets uglier...
                if (list instanceof ArrayList) {
                    // do the best we can for the common case
                    ((ArrayList) list).ensureCapacity(pos * 2);
                } else {
                    // ew...
                    while (pos++ < s) {
                        list.add(emptyItem());
                    }
                }
                list.add(value);
            }
        }
        return null;
    }

    protected T emptyItem() {
        return null;
    }

    @Override
    public T remove(int pos) {
        return list.remove(pos);
    }

    @Override
    public SizedIterator<T> iterator() {
        return SizedIterator.of(list.size(), list.iterator());
    }

    public boolean isSparse() {
        return sparse;
    }

    public void setSparse(boolean sparse) {
        this.sparse = sparse;
    }

    @Override
    public String toString() {
        if (Debuggable.debugEnabled()) {
            return "ListAdapter{" +
                "list=" + list +
                ", sparse=" + sparse +
                '}';
        }
        return String.valueOf(list);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final ListAdapter<?> that = (ListAdapter<?>) o;

        return list.equals(that.list);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    public List<T> getList() {
        return list;
    }

    @Override
    public SizedIterable<T> cached() {
        SizedIterable result = ArrayIterable.iterate(list.stream().toArray());
        return result;
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean isNotEmpty() {
        return !list.isEmpty();
    }

    @Override
    public T first() {
        return list.get(0);
    }

    @Override
    public Maybe<T> firstMaybe() {
        if (isEmpty()) {
            return Maybe.not();
        }
        return Maybe.immutable(first());
    }

    @Override
    public T firstOrNull() {
        if (isEmpty()) {
            return null;
        }
        return first();
    }

    @Override
    public T last() {
        return list.get(list.size() - 1);
    }
}

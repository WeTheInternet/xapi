package xapi.fu.java;

import xapi.fu.has.HasLock;

import java.util.List;

/**
 * ListAdapterConcurrent:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 18/09/2023 @ 2:25 a.m.
 */
public class ListAdapterConcurrent<T> extends ListAdapter<T> implements HasLock {
    public ListAdapterConcurrent() {
    }

    public ListAdapterConcurrent(final List<T> list) {
        super(list);
    }

    @Override
    public Object getLock() {
        return list;
    }
}

package xapi.util.impl;

import xapi.fu.In1Out1;
import xapi.fu.MappedIterable;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public class LinkedIterable <T> implements MappedIterable<T> {

    private final class Itr implements Iterator<T> {

        T node;
        boolean computed;

        @Override
        public boolean hasNext() {
            if (!computed) {
                computed = true;
                final T newNode = next.io(node);
                if (node == newNode) {
                    return false;
                }
                node = newNode;
            }
            return node != null;
        }

        @Override
        public T next() {
            if (!computed) {
                hasNext();
            }
            computed = false;
            if (node == null) {
                throw new NoSuchElementException();
            }
            return node;
        }
    }

    private final T head;
    private final In1Out1<T, T> next;
    private final boolean skipHead;

    public LinkedIterable(T head, In1Out1<T, T> next) {
        this(head, next, false);
    }

    public LinkedIterable(T head, In1Out1<T, T> next, boolean skipHead) {
        this.head = head;
        this.next = next;
        this.skipHead = skipHead;
    }

    @Override
    public Iterator<T> iterator() {
        final Itr itr = new Itr();
        itr.node = head;
        itr.computed = !skipHead;
        return itr;
    }
}

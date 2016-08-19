package xapi.util.impl;

import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.Rethrowable;

import static xapi.fu.Lazy.deferred1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/12/16.
 */
public class LeonardoHeap<T> {

    private interface TreeJoiner <T> {

        LeonardoTree<T> join(T value, int score, LeonardoTree<T> left, LeonardoTree<T> right);

    }

    static abstract class LeonardoTree <T> implements Cloneable, Rethrowable {

        LeonardoTree<T> next;

        T value;
        int score;
        private final TreeJoiner<T> joiner;

        private LeonardoTree(T value, int score, TreeJoiner<T> joiner) {
            this.value = value;
            this.score = score;
            this.joiner = joiner;
        }

        public LeonardoTree<T> getNext() {
            return next;
        }

        public T getValue() {
            return value;
        }

        abstract int size();

        abstract int ordinal();

        int score() {
            return score;
        }

        LeonardoTree<T> join(T value, int score) {
            final LeonardoTree<T> tree = joiner.join(value, score, next, this);
            return tree;
        }

        @Override
        protected LeonardoTree clone() {
            try {
                return (LeonardoTree) super.clone();
            } catch (CloneNotSupportedException e) {
                throw rethrow(e); // won't happen
            }
        }

        public void buildString(StringBuilder b) {
            b
                .append("{")
                .append(value)
                .append(", ")
                .append(score)
                .append("} ");
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            buildString(b);
            return b.toString();
        }
    }

    private static final class LeoHead <T> extends LeonardoTree <T> {

        private LeoHead() {
            super(null, Integer.MAX_VALUE, (ig, no, re, d)->{
                throw new UnsupportedOperationException();
            });
        }

        @Override
        int size() {
            return 0;
        }

        @Override
        int ordinal() {
            return -1;
        }
    }

    static abstract class LeonardoForest <T> extends LeonardoTree <T> {

        private final LeonardoTree<T> left;
        private final LeonardoTree<T> right;

        LeonardoForest(T value, int score, LeonardoTree<T> left, LeonardoTree<T> right, TreeJoiner<T> joiner) {
            super(value, score, joiner);
            this.left = left;
            this.right = right;
            // A forest within a forest should no longer be linked.
            left.next = null;
            right.next = null;
        }

        LeonardoTree<T> getLeft() {
            return left;
        }

        LeonardoTree<T> getMaxChild() {
            return left.score > right.score ? left : right;
        }

        LeonardoTree<T> getRight() {
            return right;
        }

        @Override
        public void buildString(StringBuilder b) {
            super.buildString(b);
            b.append("\nL(");
            getLeft().buildString(b);
            b.append(")\nR(");
            getRight().buildString(b);
            b.append(")");
        }
    }

    private static class LeoTree0 <T> extends LeonardoTree <T> {
        LeoTree0(T value, int score) {
            super(value, score, LeoTree2::new);
        }

        @Override
        int size() {
            return 1;
        }

        @Override
        int ordinal() {
            return 0;
        }
    }

    private static class LeoTree1 <T> extends LeonardoTree <T> {
        LeoTree1(T value, int score) {
            super(value, score, LeoTree3::new);
        }

        @Override
        int size() {
            return 1;
        }

        @Override
        int ordinal() {
            return 1;
        }
    }

    private static class LeoTree2 <T> extends LeonardoForest<T> {
        LeoTree2(T value, int score, LeoTree1 <T> left, LeoTree0 <T> right) {
            super(value, score, left, right, LeoTree4::new);
        }
        private LeoTree2(T value, int score, LeonardoTree <T> left, LeonardoTree <T> right) {
            this(value, score, (LeoTree1<T>)left, (LeoTree0<T>)right);
        }

        @Override
        int size() {
            return 3;
        }

        @Override
        int ordinal() {
            return 2;
        }
    }
    private static class LeoTree3 <T> extends LeonardoForest <T> {
        LeoTree3(T value, int score, LeoTree2<T> left, LeoTree1<T> right) {
            super(value, score, left, right, LeoTree5::new);
        }
        private LeoTree3(T value, int score, LeonardoTree <T> left, LeonardoTree <T> right) {
            this(value, score, (LeoTree2<T>)left, (LeoTree1<T>)right);
        }

        @Override
        int size() {
            return 5;
        }

        @Override
        int ordinal() {
            return 3;
        }
    }

    private static class LeoTree4 <T> extends LeonardoForest <T> {
        LeoTree4(T value, int score, LeoTree3<T> left, LeoTree2<T> right) {
            super(value, score, left, right, LeoTree6::new);
        }
        private LeoTree4(T value, int score, LeonardoTree <T> left, LeonardoTree <T> right) {
            this(value, score, (LeoTree3<T>)left, (LeoTree2<T>)right);
        }

        @Override
        int size() {
            return 9;
        }

        @Override
        int ordinal() {
            return 4;
        }
    }

    private static class LeoTree5 <T> extends LeonardoForest <T> {
        LeoTree5(T value, int score, LeoTree4 <T> left, LeoTree3 <T> right) {
            super(value, score, left, right, LeoTree7::new);
        }
        private LeoTree5(T value, int score, LeonardoTree <T> left, LeonardoTree <T> right) {
            this(value, score, (LeoTree4<T>)left, (LeoTree3<T>)right);
        }

        @Override
        int size() {
            return 15;
        }

        @Override
        int ordinal() {
            return 5;
        }
    }

    private static class LeoTree6 <T> extends LeonardoForest <T> {
        LeoTree6(T value, int score, LeoTree5 <T> left, LeoTree4 <T> right) {
            super(value, score, left, right, LeoTree8::new);
        }
        private LeoTree6(T value, int score, LeonardoTree <T> left, LeonardoTree <T> right) {
            this(value, score, (LeoTree5<T>)left, (LeoTree4<T>)right);
        }

        @Override
        int size() {
            return 25;
        }

        @Override
        int ordinal() {
            return 6;
        }
    }

    private static class LeoTree7 <T> extends LeonardoForest <T> {
        LeoTree7(T value, int score, LeoTree6<T> left, LeoTree5 <T> right) {
            super(value, score, left, right, LeoTreeN::merge);
        }
        private LeoTree7(T value, int score, LeonardoTree <T> left, LeonardoTree <T> right) {
            this(value, score, (LeoTree6<T>)left, (LeoTree5<T>)right);
        }
        @Override
        int size() {
            return 41;
        }

        @Override
        int ordinal() {
            return 7;
        }
    }

    private static class LeoTree8 <T> extends LeonardoForest <T> {
        LeoTree8(T value, int score, LeoTree7 <T> left, LeoTree6 <T> right) {
            super(value, score, left, right, LeoTreeN::merge);
        }

        private LeoTree8(T value, int score, LeonardoTree <T> left, LeonardoTree <T> right) {
            this(value, score, (LeoTree7<T>)left, (LeoTree6<T>)right);
        }

        @Override
        int size() {
            return 67;
        }

        @Override
        int ordinal() {
            return 8;
        }
    }

    private static class LeoTreeN <T> extends LeonardoForest <T> {
        private final int size;
        private final int ordinal;

        public static <T> LeoTreeN<T> merge(T value, int score, LeonardoTree<T> left, LeonardoTree<T> right) {
            return new LeoTreeN<>(value, score, left.size() + right.size() + 1, right.ordinal()+1, left, right);
        }

        LeoTreeN(T value, int score, int size, int ordinal, LeonardoTree <T> left, LeonardoTree <T> right) {
            super(value, score, left, right, LeoTreeN::merge);
            this.size = size;
            this.ordinal = ordinal;
            assert size == right.size() + left.size() + 1;
        }

        @Override
        int size() {
            return size;
        }

        @Override
        int ordinal() {
            return ordinal;
        }
    }

    private ObjIntConsumer<T> inserter;

    private final LeonardoTree<T> head;

    private Lazy<Iterable<T>> itr;

    public LeonardoHeap() {
        head = new LeoHead<>();
        itr = deferred1(this::buildIterator);
        inserter = (item1, score1) -> {
          final LeoTree0<T> first = new LeoTree0<>(item1, score1);
          head.next = first;
          inserter = (item2, score2) -> {
              LeoTree1<T> second;
              if (score1 > score2) {
                  // The item in slot 0 was higher; just add the new item in slot 1
                  second = new LeoTree1<>(item2, score2);
                  first.next = second;
              } else {
                  // The existing item was lower; move it over to slot 0
                  second = new LeoTree1<>(item1, score1);
                  head.next = new LeoTree0<>(item2, score2);
                  head.next.next = second;
              }
              inserter = new ObjIntConsumer<T>() {
                  @Override
                  public void accept(T t, int value) {
                      addAndBalance(t, value);
                  }
              };
          };
        };
    }

    private class Itr implements Iterator<T> {

        private LeonardoTree<T> max;

        public Itr() {
            this.max = head.next == null ? null : head.next.clone();
        }

        @Override
        public boolean hasNext() {
            return max != null;
        }

        @Override
        public T next() {
            try {
                return max.getValue();
            } finally {
                max = dequeAndBalance(max);
            }
        }
    }

    private LeonardoTree<T> dequeAndBalance(LeonardoTree<T> max) {
        if (max instanceof LeonardoForest) {
            // If this is a forest, then we need to remove the root
            // and rebalance the children.
            LeonardoForest<T> forest = (LeonardoForest<T>) max;
            final LeonardoTree<T> left = forest.getLeft();
            final LeonardoTree<T> right = forest.getRight();
            right.next = left;
            left.next = max.next;
            rebalance(left);
            rebalance(right);
            return right;
        } else {
            // If this was a 0 or 1 singleton, just return the next node
            return max.next;
        }
    }

    private Iterable<T> buildIterator() {
        return Itr::new;
    }

    /**
     * Called when there is already at least two heaps in the forest.
     */
    private void addAndBalance(T item, int score) {
        final LeonardoTree<T> first = head.next;
        final LeonardoTree<T> second = first.next;
        final LeonardoTree<T> newNode;
        if (second == null) {
            // just one heap; toss on a new 0 node and rebalance
            assert !(first instanceof LeoTree1);
            head.next = newNode = new LeoTree1<>(item, score);
            newNode.next = first;
        }
        else if (second.ordinal() == first.ordinal() + 1) {
            final LeonardoTree<T> newNext = second.next;
            newNode = first.join(item, score);
            assert newNode instanceof LeonardoForest;
            head.next = newNode;
            newNode.next = newNext;
        } else if (!(first instanceof LeonardoForest)){
            // The current head is a size 1 tree, but the next node is a larger ordinal;
            if (first.score() > score) {
                // The new item is less than the head, so we will need to balance from the new node
                head.next = new LeoTree0<>(first.value, first.score);
                head.next.next = newNode = new LeoTree1<>(item, score);
                newNode.next = second;
            } else {
                // The new item is the largest item, it will be the new head.
                head.next = newNode = new LeoTree0<>(item, score);
                newNode.next = first;
                // No balancing needed; the tree is already balanced
                return;
            }
        } else {
            newNode = new LeoTree1<>(item, score);
            newNode.next = head.next;
            head.next = newNode;
        }
        rebalance(newNode);
    }

    private void rebalance(LeonardoTree<T> newNode) {
        if (newNode.next == null) {
            shuffle(newNode);
            return;
        }
        final LeonardoTree<T> nextNode = newNode.next;
        int nextScore = nextNode.score;
        if (newNode.score < nextScore) {
            if (newNode instanceof LeonardoForest) {
                // When the newNode is a forest, we can only swap heads if the root of the
                // next tree is greater than both the left and right child of the current node.
                LeonardoForest<T> forest = (LeonardoForest<T>) newNode;
                if (forest.getLeft().score < nextScore && forest.getRight().score < nextScore) {
                    valueSwap(newNode, nextNode);
                    rebalance(nextNode);
                } else {
                    shuffle(newNode);
                }
            } else {
                // The new node is either a 0 or a 1 node
                // And, the new node is lower than its next node, so swap heads.
                valueSwap(newNode, nextNode);
                rebalance(nextNode);
            }
        } else {
            shuffle(newNode);
        }
    }

    private void shuffle(LeonardoTree<T> node) {
        if (node instanceof LeonardoForest) {
            LeonardoForest<T> forest = (LeonardoForest<T>) node;
            LeonardoTree<T> maxChild = forest.getMaxChild();
            if (node.score < maxChild.score) {
                valueSwap(node, maxChild);
                shuffle(maxChild);
            }
        }
    }

    private void valueSwap(LeonardoTree<T> newNode, LeonardoTree<T> nextNode) {
        T val = newNode.value;
        int score = newNode.score;
        newNode.value = nextNode.value;
        newNode.score = nextNode.score;
        nextNode.value = val;
        nextNode.score = score;
    }

    public void addItem(T item, int score) {
        // reset the iterable builder, to reflect our changes
        if (itr.isResolved()) {
            itr = deferred1(this::buildIterator);
        }
        inserter.accept(item, score);
    }

    public Iterable<T> forEach() {
        return itr.out1();
    }

    public static void main(String ... a) {

//        for (int i :new int[]{ 10, 0, 2, 6, 17, 15, 13, 5, 4, 8, 7, 11, 9, 3, 1}) {
//            l1.add(i);
//            l2.add(i);
//        }
        long s1=0, s2=0, s3=0;
        List<Integer> sorted = null;
        List<Integer> l1 = null, l2 = null;
        for (int j = 5000; j-->0;) {

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            l1 = new ArrayList<>();
            l2 = new ArrayList<>();
            Set<Integer> unique = new HashSet<>();
            LeonardoHeap<Integer> heap = new LeonardoHeap<>();
            In1<Integer> add = i->heap.addItem(i, i);

            int max = Integer.MAX_VALUE / 500000;
            for (int i = max/2; i-->0;) {
                int n = (int)(Math.random() * max);
                while (!unique.add(n)) {
                    n = (int)(Math.random() * max);
                }
                l1.add(n);
                l2.add(n);
            }
//            s1 = System.nanoTime();
//            Collections.sort(l1);
            s2 = System.nanoTime();
            for (Integer i : l2) {
                heap.addItem(i, i);
            }
            sorted = StreamSupport.stream(heap.forEach().spliterator(), false)
                .collect(Collectors.toList());
            s3 = System.nanoTime();
        }


        System.out.println((s3 - s2) + "\n" + (s2 - s1));
        Collections.reverse(l1);
        System.out.println(sorted.equals(l1));
        System.out.println(sorted.size());
        System.out.println(l1.size());
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (LeonardoTree<T> tree : ReverseIterable.reverse(
            new LinkedIterable<>(head, LeonardoTree::getNext, true))) {
            b.append("[");
            tree.buildString(b);
            b.append("]\n");

        }
        return b.toString();
    }
}

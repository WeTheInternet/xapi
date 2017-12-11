package xapi.ui.api;

import xapi.fu.In1Out1;
import xapi.fu.iterate.ReverseIterable;

import static xapi.util.X_Util.pushIfMissing;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/27/16.
 */
public class PhaseMap <K extends Comparable<K>> {

    private static final PhaseNode[] EMPTY = new PhaseNode[0];

    public PhaseMap() {
        instances = new LinkedHashMap<>();
    }

    public static <S, T extends Comparable<T>> PhaseMap<T> toMap(Iterable<S> source,
                                                                 In1Out1<S, T> getId,
                                                                 In1Out1<S, Integer> getPriority,
                                                                 In1Out1<S, T> getPrerequisite,
                                                                 In1Out1<S, T> getBlock) {
        PhaseMap<T> map = new PhaseMap<>();
        for (S s : source) {
            T id = getId.io(s);
            int priority = getPriority.io(s);
            T prerequisite = getPrerequisite.io(s);
            T block = getBlock.io(s);
            map.addNode(id, priority, prerequisite, block);
        }
        return map;
    }

    protected PhaseNode<K> tail, head;
    private final Map<K, PhaseNode<K>> instances;

    public PhaseNode<K> addNode(K id, int priority, K prerequisite, K block) {
        assert isWellStructured(id, prerequisite, block);
        PhaseNode<K> node = getOrCreateNode(id, priority);
        if (isAcceptable(prerequisite)) {
            PhaseNode<K> prereq= getOrCreateNode(prerequisite, priority);
            // make sure the thing that is our prerequisite knows to block us.
            prereq.block = pushIfMissing(prereq.block, node);
            node.prerequisites = pushIfMissing(node.prerequisites, prereq);
        }
        if (isAcceptable(block)) {
            PhaseNode<K> blocked = getOrCreateNode(block, priority);
            // make sure the thing that we block knows we are its prerequisite.
            blocked.prerequisites = pushIfMissing(blocked.prerequisites, node);
            node.block = pushIfMissing(node.block, blocked);
        }

        assert isWellStructured(id, prerequisite, block);
        if (head == null) {
            head = tail = node;
        } else {
            if (head.isBlockerOf(node)) {
                node.next = head;
                head = node;
                assert !tail.isPrerequisiteOf(node);
            } else  if (tail.isPrerequisiteOf(node)) {
                tail.next = node;
                tail = node;
                assert !tail.isBlockerOf(node);
            } else {
                // an insertion in the middle... lets walk the nodes to find our insertion point.
                PhaseNode<K> n = head;
                while (n != null) {
                    if (n.isBlockerOf(node) || node.isPrerequisiteOf(n)) {
                        node.next = n.next;
                        n.next = node;
                        break;
                    }
                    n = n.next;
                }
                assert n != null : "Unable to find insertion point of node " + node + " from head: " + head;
            }
        }
        return node;
    }

    protected class Itr implements Iterator<PhaseNode<K>> {

        PhaseNode<K> node = head;
        @Override
        public boolean hasNext() {
            return node != null;
        }

        @Override
        public PhaseNode<K> next() {
            try {
                return node;
            } finally {
                node = node.next;
            }
        }
    }

    public Iterable<PhaseNode<K>> forEachNode() {
        return Itr::new;
    }

    public Iterable<PhaseNode<K>> forEachNodeReverse() {
        return ReverseIterable.reverse(Itr::new);
    }

    private boolean isAcceptable(K prerequisite) {
        return prerequisite != null && !"".equals(prerequisite);
    }

    protected PhaseNode<K> getOrCreateNode(K id, int priority) {
        return instances.compute(id, (i, n)->{
           if (n == null) {
               n = new PhaseNode<>(id, priority);
           } else {
               if (priority < n.priority) {
                   n.priority = priority;
               }
           }
           return n;
        });
    }

    private boolean isWellStructured(K id, K prerequisite, K block) {
        if (!isAcceptable(id)) {
            if (isAcceptable(prerequisite) || isAcceptable(block)) {
                assert false : "Cannot have a null id unless you are root (all null pointers)";
                return false;
            }
        } else {
            // with a non-null id, we must have either a before, or an after...
            if (!isAcceptable(prerequisite)) {
                if (!isAcceptable(block)) {
                    assert false : "If you have an id (" + id +") then you must have at least one preqrequisite or one block requirement";
                    return false;
                }
            } else {
                // before != null; don't allow it to equal id
                if (prerequisite.equals(id)) {
                    assert false : "It is not legal for a phase to have prerequisite key == id key; bad key: " + prerequisite +
                          " (block: " + block + ")";
                    return false;
                }
            }
            if (isAcceptable(block)) {
                if (block.equals(id)) {
                    assert false : "It is not legal for a phase to have block key == id key; bad key: " + block +
                          " (prerequisite: " + prerequisite + ")";
                    return false;
                }
            }
        }

        if (isEmpty()) {
            return true;
        }

        return true;
    }

    public boolean isEmpty() {
        return head == tail;
    }

    public static class PhaseNode <K extends Comparable<K>> implements Comparable<PhaseNode<K>> {

        PhaseNode<K> next;

        PhaseNode<K>[] prerequisites;
        PhaseNode<K>[] block;
        Map<K, Integer> relations;
        K id;
        int priority;

        PhaseNode(K id, int priority) {
            this.id = id;
            this.priority = priority;
            relations = createRelationMap();
            prerequisites = block = EMPTY;
        }

        public PhaseNode<K>[] getPrerequisites() {
            return prerequisites;
        }

        public PhaseNode<K>[] getBlock() {
            return block;
        }

        public K getId() {
            return id;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(PhaseNode<K> o) {
            if (this.equals(o)) {
                return 0;
            }
            return relations.computeIfAbsent(o.id, id -> {
                int relation = 0;

                boolean iBlockYou = isBlocking(o);

//                if (prerequisite.get(o.id) != null) {
//                    if (o.prerequisite.get(id) != null) {
//                        throw new IllegalStateException("Phases " + id + " and "
//                              + o.id + " cannot both be prerequisite of each other");
//                    }
//                    o.block.getOrCreate(id, i -> this);
//                    prerequisite.putAll(o.prerequisite.entries());
//                    o.block.putAll(block.entries());
//                    relation = 1;
//                }
//                if (block.get(o.id) != null) {
//                    if (o.block.get(id) != null) {
//                        throw new IllegalStateException("Phases " + id + " and "
//                              + o.id + " cannot both block each other");
//                    }
//                    o.prerequisite.getOrCreate(id, i -> this);
//                    block.putAll(o.block.entries());
//                    o.prerequisite.putAll(prerequisite.entries());
//                    assert relation != 1;
//                    relation = -1;
//                }
//
//                if (o.prerequisite.get(id) != null) {
//                    block.getOrCreate(id, i -> o);
//                    o.prerequisite.putAll(prerequisite.entries());
//                    block.putAll(o.block.entries());
//                    assert relation != 1;
//                    relation = -1;
//                }
//                if (o.block.get(id) != null) {
//                    prerequisite.getOrCreate(id, i -> o);
//                    o.block.putAll(block.entries());
//                    prerequisite.putAll(o.prerequisite.entries());
//                    assert relation != -1;
//                    relation = 1;
//                }
//
//                if (relation == 0) {
//                    for (PhaseNode node : prerequisite.values()) {
//                        if (o.block.get(node.id) != null) {
//                            // o blocks our prerequisite.
//                            relation = 1;
//                            break;
//                        }
//                    }
//                }
//                if (relation == 0) {
//                    for (PhaseNode node : block.values()) {
//                        if (o.prerequisite.get(node.id) != null) {
//                            relation = -1;
//                            break;
//                        }
//                    }
//                }
//
//                if (relation == 0) {
//                    for (PhaseNode node : o.prerequisite.values()) {
//                        if (block.get(node.id) != null) {
//                            // we blocks one of o's prerequisite.
//                            relation = -1;
//                            break;
//                        }
//                    }
//                }
//                if (relation == 0) {
//                    for (PhaseNode node : o.block.values()) {
//                        if (prerequisite.get(node.id) != null) {
//                            // we require a node that o blocks.
//                            relation = 1;
//                            break;
//                        }
//                    }
//                }
//                if (relation == 0) {
//                    // these nodes are in the same phase...
//                    relation = Integer.compare(priority, o.priority);
//                    if (relation == 0) {
//                        relation = id.compareTo(o.id);
//                    }
//                }
//
//                assert relation != 0;
                return relation;
            });
        }

        protected boolean isBlocking(PhaseNode<K> o) {
            PhaseNode<K>[] cur = block;
            if (cur == EMPTY) {
                return false;
            }
            Set<K> seen = new HashSet<>();
            return deepContains(seen, o, PhaseNode::getBlock, cur);
        }

        protected boolean isPrerequisite(PhaseNode<K> o) {
            PhaseNode<K>[] cur = prerequisites;
            if (cur == EMPTY) {
                return false;
            }
            Set<K> seen = new HashSet<>();
            return deepContains(seen, o, PhaseNode::getPrerequisites, cur);
        }

        protected final boolean isBlockerOf(PhaseNode<K> o) {
            return o.isBlocking(this);
        }

        protected final boolean isPrerequisiteOf(PhaseNode<K> o) {
            return o.isPrerequisite(this);
        }

        public boolean deepContains(Set<K> seen, PhaseNode<K> o, In1Out1<PhaseNode<K>, PhaseNode<K>[]> target, PhaseNode<K>[] cur) {
            if (cur == EMPTY || cur == null || cur.length == 0) {
                return false;
            }
            for (PhaseNode<K> node : cur) {
                // prevent recursion sickness
                if (Objects.equals(o.id, node.id)) {
                    return true;
                }
                if (seen.add(node.id)) {
                  final PhaseNode<K>[] nexts = target.io(node);
                  if (node.deepContains(seen, o, target, nexts)) {
                      return  true;
                  }
                }
            }

            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof PhaseNode))
                return false;

            final PhaseNode phaseNode = (PhaseNode) o;

            return id != null ? id.equals(phaseNode.id) : phaseNode.id == null;

        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "PhaseNode{" +
                  "priority=" + priority +
                  ", id=" + id +
                  ", next=" + next +
                  '}';
        }

        protected Map<K, Integer> createRelationMap() {
            return new LinkedHashMap<>();
        }
    }

    public static PhaseMap<String> withDefaults(Set<UiPhase> phases) {

        assert phases.getClass() != HashSet.class :
            "Do not send a hashset for these order-dependent values";
        for (Class<?> phase : UiPhase.CORE_PHASES) {
            phases.add(phase.getAnnotation(UiPhase.class));
        }

        // compute phases to run
        return toMap(phases, UiPhase::id, UiPhase::priority, UiPhase::prerequisite, UiPhase::block);
    }
}

package xapi.ui.api;

import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.iterate.EmptyIterator;
import xapi.util.impl.LinkedIterable;

/**
 * The RangePool class is used to store a set of {@link Coord}s,
 * used as ranges, where X and Y are used to denote
 * a pair of start/end points along the same dimension
 * (X and Y are named points, not the names of intersecting dimensions).
 *
 *
 * That is, this class represents a +Infinity to -Infinity range of space,
 * which can then be reserved by calling code in an efficient manner.
 *
 * The internal structure of this collection is a linked list
 * of reserved and unreserved spaces (for visualization purposes,
 * think of reserved space in red, and unreserved in green).
 *
 * We start with a single node of unreserved space from +Infinity to -Infinity.
 * To request a range to reserve, calling code sends a Coord,
 * and we check if that space is reserved or not;
 * if not, we reserve that space by splitting the unreserved node,
 * and splicing in a reserved space to take its place.
 *
 * Then, we will want to update our min/max reserved point pointers
 * (yes, point pointers. We are using points, and storing pointers to them),
 * so we can efficiently handle any reservation request that is closer to
 * infinity in either direction than any reserved point.
 *
 * Any reservation requests made within that range
 * should be smart enough to search from the nearest point possible.
 *
 * Imagine we have the following spaces (for unreserved), [for reserved]:
 *
 *  (-Infinity, -1)
 *  [-1, 0]
 *  (0, 1)
 *  [1, 3]
 *  (3, 5)
 *  [5, 9]
 *  (9, +Infinity)
 *
 *  This pool has three reserved spaces, with some space in between them.
 *  There are 7 nodes in total (2n + 1).  By storing the highest and lowest point,
 *  we can quickly handle any requests beyond either edge (outside -1 to +9 is easy).
 *  However, when we want to insert something in the middle, we will need
 *  a heuristic that can be fast at finding the best insertion point quickly.
 *
 *  To do this, we will need a way to find a middle point in the linked list;
 *  rather than pay a lot to continually update binary search pointers,
 *  we can instead also store a linked list based on insertion order,
 *  so that when seeking a starting point, we can consider min, max, head and tail.
 *
 *  Whichever starting point has the nearest edge, we can start our search from there.
 *
 *  When searching within nodes, we will want to consider reserved spaces next to
 *  each other as a single reserved space during the first pass on node-search
 *  (so we can logarithmically reduce the search space when nodes often touch each other).
 *
 *  To do this, each node will maintain a higherReserved and higherUnreserved pointers;
 *  by searching for "the closest end of the next unreserved space",
 *  we can skip all adjacent reserved spaces, while still being able to maintain
 *  a generalized linked list structure.
 *
 *  This collection will allow you to iterate elements in ascending or descending order,
 *  as well as by insertion order (when insertion order corresponds to priority,
 *  and when your calling code adapts to space reservation failures by shrinking
 *  or moving the reservation request, it becomes important to request space
 *  in the correct order).
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 3/19/17.
 */
public class RangePool <T> {

    public static class RangeNode<T> {

        protected Coord range;
        protected ReservedNode<T> lowerReserved, higherReserved;
        protected Out1<UnreservedNode<T>> lowerUnreserved, higherUnreserved;

        public RangeNode(Coord range) {
            this.range = range;
        }

        public ReservedNode<T> getLowerReserved() {
            return lowerReserved;
        }

        public ReservedNode<T> getHigherReserved() {
            return higherReserved;
        }

        public UnreservedNode<T> getLowerUnreserved() {
            return lowerUnreserved.out1();
        }

        public UnreservedNode<T> getHigherUnreserved() {
            return higherUnreserved.out1();
        }

        public Coord getRange() {
            return range;
        }

        public Maybe<UnreservedNode<T>> findUnreserved(Coord coord) {
            final UnreservedNode<T> lower = getLowerUnreserved();
            if (lower != this &&
                (
                    lower.getRange().getY() >= coord.getY()
                )
            ){
                return lower.findUnreserved(coord);
            }
            final UnreservedNode<T> higher = getHigherUnreserved();
            if (higher != this &&
                (
                    higher.getRange().getX() <= coord.getX()
                )
            ) {
                return higher.findUnreserved(coord);
            }
            return Maybe.not();
        }
    }
    public enum RangeDirection {
        Higher, Lower
    }
    public static class UnreservedNode <T> extends RangeNode <T> {

        protected Mutable<UnreservedNode<T>> fromAbove, fromBelow;

        public UnreservedNode(Coord range, UnreservedNode<T> from, RangeDirection dir) {
            this(range);
            // First, make ourselves look just like the source node, except for our shared pointers
            lowerUnreserved = from.lowerUnreserved;
            higherUnreserved = from.higherUnreserved;
            lowerReserved = from.lowerReserved;
            higherReserved = from.higherReserved;

            // Fix the mutable pointers based on the direction we are supposed to fill
            if (dir == RangeDirection.Higher) {
                // If the requested direction was higher, then this new node should be on the bottom
                // we will take over the previous unreserved node's pointer, and give it our unused one.
                final Mutable<UnreservedNode<T>> move = fromBelow;
                fromBelow = from.fromBelow;
                fromBelow.set(this);
                from.fromBelow = move;
                move.set(from);
                higherUnreserved = from.fromBelow;
                from.lowerUnreserved = fromAbove;
            } else {
                // If the requested direction was lower, then this new node should be on the top
                final Mutable<UnreservedNode<T>> move = fromAbove;
                fromAbove = from.fromAbove;
                fromAbove.set(this);
                from.fromAbove = move;
                move.set(from);
                lowerUnreserved = from.fromAbove;
                from.higherUnreserved = fromBelow;
            }
        }
        public UnreservedNode(Coord range) {
            super(range);
            assert range.getX() <= range.getY();
            lowerUnreserved = fromBelow = new Mutable<>(this);
            higherUnreserved = fromAbove = new Mutable<>(this);
        }

        public UnreservedNode<T> split(ReservedNode<T> node, RangeDirection dir) {
            final Coord me = getRange();
            final Coord you = node.getRange();

            assert me.contains(you) : "Attempting to split an unreserved node using space not owned by us;" +
                " Me: " + me + "; requested: " + you ;

            final Coord newBottom = Coord.coord(me.getX(), you.getX());
            final Coord newTop = Coord.coord(you.getY(), me.getY());

            assert newBottom.size() > 0 : "Do not use split when inserting on the bottom edge of an UnreservedNode";
            assert newTop.size() > 0 : "Do not use split when inserting on the top edge of an UnreservedNode";

            // Update our range.  The split node stays toward the bottom.
            // We may want to make the dir argument affect which side the current
            // node stays on.
            UnreservedNode<T> higher, lower, newNode;

            if (dir == RangeDirection.Higher) {
                range = newTop;
                newNode = new UnreservedNode<>(newBottom, this, dir);
                higher = this;
                lower = newNode;
            } else {
                range = newBottom;
                newNode = new UnreservedNode<>(newTop, this, dir);
                higher = newNode;
                lower = this;
            }


            // Update the new node's pointers first.
            // if this node is already attached somewhere,
            // we detach it first, so we don't create cycles in the graph...
            node.detach();
            node.lowerReserved = getLowerReserved();
            if (node.lowerReserved != null) {
                node.lowerReserved.higherReserved = node;
            }
            node.higherReserved = getHigherReserved();
            if (node.higherReserved != null) {
                node.higherReserved.lowerReserved = node;
            }
            node.higherUnreserved = higher.fromBelow;
            node.lowerUnreserved = lower.fromAbove;

            if (dir == RangeDirection.Higher) {
                lowerReserved = node;
                newNode.higherReserved = node;

            } else {
                higherReserved = node;
                newNode.lowerReserved = node;
            }
            return newNode;
        }

        public void attachAbove(ReservedNode<T> node) {
            final Coord me = getRange();
            final Coord you = node.getRange();
            assert you.getY() == me.getY() : "Cannot attach above if upper values are not equal";
            // slide this node down to end at the beginning of the newly attached node
            range = Coord.coord(me.getX(), you.getX());
            if (range.isZero()) {
                // remove this unreserved node entirely.
                replaceWith(node);
            } else {
                node.lowerUnreserved = fromAbove;
                node.higherUnreserved = getHigherUnreserved().fromBelow;
                node.lowerReserved = getLowerReserved();
                if (lowerReserved != null) {
                    lowerReserved.higherReserved = node;
                }
                node.higherReserved = getHigherReserved();
                if (higherReserved != null) {
                    higherReserved.lowerReserved = node;
                }
                higherReserved = node;
            }
        }
        public void attachBelow(ReservedNode<T> node) {
            final Coord me = getRange();
            final Coord you = node.getRange();
            assert you.getX() == me.getX() : "Cannot attach below if lower values are not equal";
            // slide this node up to begin at the end of the newly attached node
            range = Coord.coord(you.getY(), me.getY());
            if (range.isZero()) {
                // remove this unreserved node entirely.
                replaceWith(node);
            } else {
                node.higherUnreserved = fromBelow;
                node.lowerUnreserved = getLowerUnreserved().fromAbove;
                node.lowerReserved = getLowerReserved();
                if (lowerReserved != null) {
                    lowerReserved.higherReserved = node;
                }
                node.higherReserved = getHigherReserved();
                if (node.higherReserved != null) {
                    node.higherReserved.lowerReserved = node;
                }
                lowerReserved = node;
            }
        }

        public Maybe<UnreservedNode<T>> findUnreserved(Coord coord) {
            if (getRange().intersects(coord)) {
                return Maybe.immutable(this);
            }
            return super.findUnreserved(coord);
        }

        public void replaceWith(ReservedNode<T> node) {
            final Coord coord = node.getRange();
            assert getRange().equals(coord) : "Do not replace an unreseved node with a node of different size";

            // Update unreserved nodes
            final UnreservedNode<T> lower = getLowerUnreserved();
            final UnreservedNode<T> higher = getHigherUnreserved();
            assert lower != this : "Do not reserve -Infinity (" + node.getRange()+" : " + this + ")";
            assert higher != this : "Do not reserve +Infinity (" + node.getRange() + " : " + this + ")";
            fromBelow.in(lower);
            fromAbove.in(higher);
            lower.higherUnreserved = higher.fromBelow;
            higher.lowerUnreserved = lower.fromAbove;

            // Update reserved nodes
            final ReservedNode<T> before = getLowerReserved();
            final ReservedNode<T> after = getHigherReserved();
            assert before.getHigherReserved() == after;
            assert after.getLowerReserved() == before;
            before.higherReserved = node;
            node.lowerReserved = before;
            after.lowerReserved = node;
            node.higherReserved = after;

        }
    }
    public static class ReservedNode<T> extends RangeNode {
        private final T value;

        public ReservedNode(Coord range, T value) {
            super(range);
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        // Prototype too complex already to worry about insertion ordered linking
//
//        public ReservedNode<T> getBefore() {
//            return before;
//        }
//
//        public ReservedNode<T> getAfter() {
//            return after;
//        }

        public void detach() {
            // TODO: if attached, remove this node and stitch the hole up correctly.
            if (isAttached()) {

            }
        }

        private boolean isAttached() {
            return false;
        }
    }

    private ReservedNode<T> head, tail;

    private UnreservedNode<T> highest, lowest;

    public RangePool() {
        highest = lowest = new UnreservedNode<>(Coord.INFINITY);
    }

    public boolean insert(Coord coord, T value) {

        ReservedNode<T> newNode = new ReservedNode<>(coord, value);
        if (highest.getRange().contains(coord)) {
            // we can insert the coordinate into to top range.
            if (highest.getRange().getX() == coord.getX()) {
                // the new node will slide the unreserved node up
                highest.attachBelow(newNode);
            } else {
                // the new node will split the unreserved node
                // note: WE RE-ASSIGN LOWEST,
                final UnreservedNode<T> newSpace = highest.split(newNode, RangeDirection.Higher);
                if (lowest == highest) {
                    lowest = newSpace;
                }
            }
            return true;
        } else if (lowest.getRange().contains(coord)) {
            // we can insert the coordinate into to bottom range.
            if (lowest.getRange().getY() == coord.getY()) {
                lowest.attachAbove(newNode);
            } else {
                final UnreservedNode<T> newSpace = lowest.split(newNode, RangeDirection.Lower);
                if (lowest == highest) {
                    // This should probably never happen, since the case of both ==
                    // should be from -Infinity to +Infinity
                    highest = newSpace;
                }
            }
            return true;
        } else {
            // we have to try to insert the node somewhere in the middle.
            // for now, we'll just use highest/lowest and pay to iterate unreserved nodes
            double toHighest = highest.getRange().getX() - coord.getY();
            double toLowest = coord.getX() - lowest.getRange().getY();
            final UnreservedNode<T> searchFrom;
            if (toHighest > toLowest) {
                searchFrom = lowest;
            } else {
                searchFrom = highest;
            }
            Maybe<UnreservedNode<T>> into = searchFrom.findUnreserved(coord);
            if (into.isPresent()) {
                final UnreservedNode<T> space = into.get();
                final Coord unreserved = space.getRange();
                if (unreserved.contains(coord)) {
                    // This unreserved space can wholly contain the new coord
                    if (unreserved.equals(coord)) {
                        // The entire unreserved space was taken.  Remove this node entirely
                        space.replaceWith(newNode);
                    } else if (unreserved.getY() == coord.getY()) {
                        space.attachAbove(newNode);
                    } else if (unreserved.getX() == coord.getX()) {
                        space.attachBelow(newNode);
                    } else {
                        space.split(newNode, RangeDirection.Higher);
                    }
                    return true;
                } else {
                    // The unreserved space overlaps us, but does not have enough room
                    // TODO: allow coord shifting / snapping into place
                }
            }
        }

        return false;
    }

    public Maybe<Coord> reserveSpace(Coord point, T value) {

        return Maybe.not();
    }

    public boolean isValid() {
        // This should only ever be called in assertions.
        // Lets check that all our nodes obey the invariants of this data structure.
        if (lowest.getLowerReserved() != null) {
            return false;
        }
        if (highest.getHigherReserved() != null) {
            return false;
        }
        if (lowest == highest) {
            // when lowest equals highest, they better be empty...
            if (lowest.getHigherReserved() != null) {
                return false;
            }
            if (highest.getLowerReserved() != null) {
                return false;
            }
        }

        return true;
    }

    public MappedIterable<UnreservedNode<T>> forUnreservedAsc() {
        return new LinkedIterable<>(lowest, UnreservedNode::getHigherUnreserved);
    }

    public MappedIterable<UnreservedNode<T>> forUnreservedDesc() {
        return new LinkedIterable<>(highest, UnreservedNode::getLowerUnreserved);
    }

    public MappedIterable<ReservedNode<T>> forReservedAsc() {
        if (lowest.getHigherReserved() == null) {
            return EmptyIterator.none();
        }
        return new LinkedIterable<>(lowest.getHigherReserved(), RangeNode::getHigherReserved);
    }

    public MappedIterable<ReservedNode<T>> forReservedDesc() {
        if (highest.getLowerReserved() == null) {
            return EmptyIterator.none();
        }
        return new LinkedIterable<>(highest.getLowerReserved(), RangeNode::getLowerReserved);
    }
}

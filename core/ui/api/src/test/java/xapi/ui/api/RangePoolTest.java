package xapi.ui.api;

import org.junit.Test;
import xapi.fu.iterate.ArrayIterable;
import xapi.ui.api.RangePool.ReservedNode;
import xapi.ui.api.RangePool.UnreservedNode;
import xapi.util.impl.ReverseIterable;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static xapi.ui.api.Coord.coord;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 3/19/17.
 */
public class RangePoolTest {

    @Test
    public void testInsertAtEdges() {
        int cnt = 0;
        RangePool<Integer> pool = new RangePool<>();
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)}
        );

        pool.insert(coord(-2, 2), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, -2), coord(2, Double.POSITIVE_INFINITY)},
            coord(-2, 2)
        );

        // Now, insert a node directly above
        pool.insert(coord(2, 4), cnt++);
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -2),
                coord(4, Double.POSITIVE_INFINITY)
            },
            coord(-2, 2),
            coord(2, 4)
        );

        // Now, insert a node directly below
        pool.insert(coord(-4, -2), cnt++);
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, -4), coord(4, Double.POSITIVE_INFINITY)},
            coord(-4, -2),
            coord(-2, 2),
            coord(2, 4)
        );

        // Do the same thing, but invert the order of higher/lower
        pool = new RangePool<>();
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)}
        );

        pool.insert(coord(-2, 2), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, -2), coord(2, Double.POSITIVE_INFINITY)},
            coord(-2, 2)
        );

        // Now, insert a node directly below
        pool.insert(coord(-4, -2), cnt++);
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -4),
                coord(2, Double.POSITIVE_INFINITY)
            },
            coord(-4, -2),
            coord(-2, 2)
        );

        // Now, insert a node directly above
        pool.insert(coord(2, 4), cnt++);
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -4),
                coord(4, Double.POSITIVE_INFINITY)
            },
            coord(-4, -2),
            coord(-2, 2),
            coord(2, 4)
        );
    }

    @Test
    public void testInsertAtContainedEdges() {
        int cnt = 0;
        RangePool<Integer> pool = new RangePool<>();
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)}
        );

        // First, lets create some outer boundaries for us to work within.

        pool.insert(coord(8, 16), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(8, 16)
        );

        pool.insert(coord(-16, -8), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(-8, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(8, 16)
        );

        // now, lets split the sectioned off area in the middle
        pool.insert(coord(-1, 1), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(-8, -1),
                coord(1, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(-1, 1),
            coord(8, 16)
        );

        // now, insert items at the top and bottom edges of the new middle reserved node
        pool.insert(coord(-2, -1), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(-8, -2),
                coord(1, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(-2, -1),
            coord(-1, 1),
            coord(8, 16)
        );
        pool.insert(coord(1, 2), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(-8, -2),
                coord(2, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(-2, -1),
            coord(-1, 1),
            coord(1, 2),
            coord(8, 16)
        );

        // now, lets select the remaining unselected portions, and ensure they are removed
        pool.insert(coord(-8, -2), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(2, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(-8, -2),
            coord(-2, -1),
            coord(-1, 1),
            coord(1, 2),
            coord(8, 16)
        );
        pool.insert(coord(2, 8), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(-8, -2),
            coord(-2, -1),
            coord(-1, 1),
            coord(1, 2),
            coord(2, 8),
            coord(8, 16)
        );

    }

    @Test
    public void testRangeSplit() {
        int cnt = 0;
        RangePool<Integer> pool = new RangePool<>();
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)}
        );

        pool.insert(coord(8, 16), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, 8), coord(16, Double.POSITIVE_INFINITY)},
            coord(8, 16)
        );

        // force another split, this time, with only one neighbor
        pool.insert(coord(-16, -8), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(-8, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(8, 16)
        );

        pool.insert(coord(-2, 2), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(-8, -2),
                coord(2, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(-2, 2),
            coord(8, 16)
        );

        // do it again, but this time reverse the side we do the second split upon
        pool = new RangePool<>();
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)}
        );

        pool.insert(coord(-16, -8), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{coord(Double.NEGATIVE_INFINITY, -16), coord(-8, Double.POSITIVE_INFINITY)},
            coord(-16, -8)
        );

        // force another split, this time, with only one neighbor
        pool.insert(coord(8, 16), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(-8, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(8, 16)
        );

        pool.insert(coord(-2, 2), cnt++);
        // run asserts on pool contents
        assertPoolStructure(pool,
            new Coord[]{
                coord(Double.NEGATIVE_INFINITY, -16),
                coord(-8, -2),
                coord(2, 8),
                coord(16, Double.POSITIVE_INFINITY)
            },
            coord(-16, -8),
            coord(-2, 2),
            coord(8, 16)
        );

    }

    private void assertPoolStructure(RangePool<Integer> pool, Coord[] unreserved, Coord ... reserved) {
        assertTrue("", pool.isValid());
        Coord[] actualUnreserved = pool.forUnreservedAsc().map(UnreservedNode::getRange)
            .toArray(Coord[]::new);
        assertArrayEquals(unreserved, actualUnreserved);
        actualUnreserved = pool.forUnreservedDesc().map(UnreservedNode::getRange)
            .toArray(Coord[]::new);
        unreserved = ReverseIterable.reverse(ArrayIterable.iterate(unreserved))
                    .toArray(Coord[]::new);
        assertArrayEquals(unreserved, actualUnreserved);

        Coord[] actualReserved = pool.forReservedAsc().map(ReservedNode::getRange)
            .toArray(Coord[]::new);
        assertArrayEquals(reserved, actualReserved);
        actualReserved = pool.forReservedDesc().map(ReservedNode::getRange)
            .toArray(Coord[]::new);
        reserved = ReverseIterable.reverse(ArrayIterable.iterate(reserved))
                    .toArray(Coord[]::new);
        assertArrayEquals(reserved, actualReserved);
    }

}

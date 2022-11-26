package xapi.fu.itr;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ChainIterationTest:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 30/10/2022 @ 12:41 a.m.
 */
public class ChainIterationTest {

    @Test
    public void chainCanBeClearedWithRemove() {
        final Chain<Integer> chain = Chain.<Integer>startChain().add(1).addAll(2, 3).build();
        assertEquals("1,2,3", chain.join(","));
        Iterator<Integer> itr = Chain.iteratorStatic(chain);
        assertEquals(new Integer(1), itr.next());
        itr.remove();
        assertEquals("2,3", chain.join(","));
        assertEquals(new Integer(2), itr.next());
        assertEquals(new Integer(3), itr.next());
        itr.remove();
        assertEquals("2", chain.join(","));
        itr = Chain.iteratorStatic(chain);
        assertEquals("2", chain.join(","));
        assertEquals(new Integer(2), itr.next());
        itr.remove();
        assertTrue(chain.isEmpty());

    }
}

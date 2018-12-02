package xapi.fu.itr;

import org.junit.Test;
import xapi.fu.itr.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/28/16.
 */
public class MappedIterableTest {

    @Test
    public void testEmptyIterable() {
        boolean failed = false;
        for (Object o : new Chain<>()) {
            failed = true;
        }
        assertFalse("Empty chain not empty", failed);
        for (Object o : Chain.startChain()) {
            failed = true;
        }
        assertFalse("Empty ChainBuilder not empty", failed);

        for (Object o : Chain.startChain().build()) {
            failed = true;
        }
        assertFalse("Empty built Chain not empty", failed);

    }

    @Test
    public void testChainInsert() {
        boolean failed = false;
        Chain<Object> c = new Chain<>();
        c.insert("0");
        c.insert(1);
        assertIterableEquals(c, 1, "0");

    }

    @Test
    public void testIterables() {
        List<String> list = Arrays.asList("1", "2", "3");
        ChainBuilder<String> builder = Chain.toChain("1", "2", "3");
        Chain<String> chain = builder.build();
        Iterator<String> itr = list.iterator();
        assertThat(itr).containsExactly("1", "2", "3");
        // double-tap, to ensure the iterable isn't holding state
        assertIterableEquals(list, "1", "2", "3");
        assertIterableEquals(builder, "1", "2", "3");
        assertIterableEquals(chain, "1", "2", "3");
    }
    @Test
    public void testReduce() {
        MappedIterable<String> reduced;
        ChainBuilder<String> build1 = Chain.toChain("1", "2", "3");
        final Chain<String> chain1 = build1.build();
        ChainBuilder<String> build2 = Chain.toChain("4", "5", "6");
        final Chain<String> chain2 = build2.build();

        assertIterableEquals(build1, "1", "2", "3");
        assertIterableEquals(chain1, "1", "2", "3");

        assertIterableEquals(build2, "4", "5", "6");
        assertIterableEquals(chain2, "4", "5", "6");

        ChainBuilder<Iterable<String>> build3 = Chain.toChain(build1, build2);
        ChainBuilder<Iterable<String>> chain3 = Chain.toChain(chain1, chain2);

        reduced = build3.flatten(MappedIterable::mapped);
        assertIterableEquals(reduced, "1", "2", "3", "4", "5", "6");
        reduced = chain3.flatten(MappedIterable::mapped);
        assertIterableEquals(reduced, "1", "2", "3", "4", "5", "6");

        // Appending second chain to first chain will cause linked chains to duplicate the second item
        build1.addAll(build2);

        reduced = build3.flatten(MappedIterable::mapped);
        assertIterableEquals(reduced, "1", "2", "3", "4", "5", "6", "4", "5", "6");
        reduced = chain3.flatten(MappedIterable::mapped);
        assertIterableEquals(reduced, "1", "2", "3", "4", "5", "6", "4", "5", "6");


        ChainBuilder<Iterable<String>> build4 = Chain.toChain(build1, build2);
        ChainBuilder<Iterable<String>> chain4 = Chain.toChain(chain1, chain2);

        assertIterableEquals(build1, "1", "2", "3", "4", "5", "6");
        assertIterableEquals(chain1, "1", "2", "3", "4", "5", "6");

        assertIterableEquals(build2, "4", "5", "6");
        assertIterableEquals(chain2, "4", "5", "6");

        reduced = build4.flatten(MappedIterable::mapped);
        assertIterableEquals(reduced, "1", "2", "3", "4", "5", "6", "4", "5", "6");
        reduced = chain4.flatten(MappedIterable::mapped);
        assertIterableEquals(reduced, "1", "2", "3", "4", "5", "6", "4", "5", "6");


        ChainBuilder<Iterable<String>> build5 = Chain.toChain(build1, build2);
        ChainBuilder<Iterable<String>> chain5 = Chain.toChain(build1, build2);

    }

    @Test
    public void testCachingIterator() {
        ChainBuilder<String> vals = Chain.toChain("one", "two", "three");
        assertIterableEquals(vals, "one", "two", "three");
        final MappedIterable<String> cached = CachingIterator.cachingIterable(vals.iterator());
        assertIterableEquals(cached, "one", "two", "three");
        assertIterableEquals(cached, "one", "two", "three");

    }

    @Test
    public void testCountedIterator() {
        final ChainBuilder<Integer> vals = Chain.toIntChain(1, 2, 3);
        final SizedIterable<Integer> counted = vals.map(i -> i + 1)
            .counted();
        assertThat(counted.size()).isEqualTo(3);
        assertThat(counted).containsExactly(2, 3, 4);
        assertThat(counted).containsExactly(2, 3, 4);

        final MappedIterable<Integer> itr = SingletonIterator.singleItem(1)
            .map(i -> i + 1);
        assertThat(itr.isEmpty()).isFalse();
        assertThat(itr).containsExactly(2);
        assertThat(itr).containsExactly(2);
    }

    private <T> void assertIterableEquals(Iterable<T> itr, T ... contents) {
        assertThat(itr.iterator()).containsExactly(contents);
        // double-tap is intentional; we want to detect if an iterable is maintaining state.
        assertThat(itr.iterator()).containsExactly(contents);

    }
}

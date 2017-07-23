package xapi.fu.iterate;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/28/16.
 */
public class SizedIterableTest {

    @Test
    public void testSlicedIterable() {
        boolean failed = false;
        final Iterable<String> source = Chain.<String>startChain().add("1").add("2");
        SizedIterable<String> itr = SizedIterable.of(2, source);

        SizedIterable<String> result = itr.mergeInsert(1, "1a");

        assertIterableEquals(result, "1", "1a", "2");

        result = itr.mergeInsert(0, "0");
        assertIterableEquals(result, "0", "1", "2");

        result = itr.mergeInsert(2, "3");
        assertIterableEquals(result, "1", "2", "3");

    }

    private <T> void assertIterableEquals(Iterable<T> itr, T ... contents) {
        assertThat(itr.iterator()).containsExactly(contents);
        // double-tap is intentional; we want to detect if an iterable is maintaining state.
        assertThat(itr.iterator()).containsExactly(contents);

    }
}

package xapi.collect.impl;

import org.junit.Test;
import xapi.collect.X_Collect;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/6/16.
 */
public class ReverseIterableTest {

  @Test
  public void testSimpleIterable() {
    final List<String> expected = Arrays.asList("3", null, "2", "1");
    Iterable<String> source = Arrays.asList("1", "2", null, "3");
    List<String> actual = new ArrayList<>();
    X_Collect.reverse(source, actual::add);
    assertEquals(expected, actual);
  }

  @Test
  public void testSimpleIterator() {
    final List<String> expected = Arrays.asList("3", null, "2", "1");
    Iterator<String> source = Arrays.asList("1", "2", null, "3").iterator();
    List<String> actual = new ArrayList<>();
    X_Collect.reverse(source, actual::add);
    assertEquals(expected, actual);
  }
}

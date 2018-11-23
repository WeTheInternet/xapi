/**
 *
 */
package xapi.test.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import xapi.collect.impl.AbstractLinkedList;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public abstract class AbstractLinkedListTest<L extends AbstractLinkedList<String, ?, L>> {
  final L stack = newList();

  @Before
  public void before() {
    stack.clear();
    stack.add("one").add("two");
  }

  @Test
  public void testClear() {
    assertFalse(stack.isEmpty());
    stack.clear();
    assertTrue(stack.isEmpty());
    final Iterator<String> iter = stack.iterator();
    assertFalse(iter.hasNext());
    for (final String v : stack) {
      throw new AssertionError("Expect stack to be empty");
    };
  }

  @Test
  public void testConsume() {
    final L stack2 = newList();
    stack2.add("three");
    stack.consume(stack2);
    assertEquals(stack.head(), "one");
    assertEquals(stack.tail(), "three");
    assertTrue(stack2.isEmpty());
    assertNull(stack2.head());
    assertNull(stack2.tail());
    assertIteratorContents(stack.iterator(), "one", "two", "three");
  }

  @Test
  public void testForEach() {
    final String[] expected = new String[] {
        "one"
    };
    for (final String s : stack) {
      assertEquals(expected[0], s);
      expected[0] = "two";
    };
  }

  @Test
  public void testHeadAndTail() {
    assertEquals("one", stack.head());
    assertEquals("two", stack.tail());
    stack.add("three");
    assertEquals("three", stack.tail());
  }

  @Test
  public void testIterate() {
    assertIteratorContents(stack.iterator(), "one", "two");
  }

  @Test
  public void testJoin() {
    assertEquals("one, two", stack.join(", "));
  }

  protected abstract L newList();

  void assertIteratorContents(final Iterator<String> iter,
    final String... expecteds) {
    for (final String expected : expecteds) {
      assertTrue(iter.hasNext());
      assertEquals(expected, iter.next());
    }
    assertFalse(iter.hasNext());
  }
}

/**
 *
 */
package xapi.test.collect;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.Test;

import xapi.collect.impl.SimpleLinkedList;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class SimpleLinkedListTest extends
AbstractLinkedListTest<SimpleLinkedList<String>> {

  @Test
  public void testListIterator_Add() {
    final ListIterator<String> listItr = stack.listIterator();
    listItr.next();
    listItr.add("one.five");
    assertIteratorContents(stack.iterator(), "one", "one.five", "two");
    assertIteratorContents(stack.listIterator(), "one", "one.five", "two");
    assertIteratorContents(stack.iteratorReverse(), "two", "one.five", "one");
    assertEquals("one", stack.head());
    assertEquals("two", stack.tail());
  }

  @Test
  public void testListIterator_AddEnd() {
    ListIterator<String> listItr = stack.listIterator();
    assertIteratorContents(listItr, "one", "two");
    listItr.add("three");
    assertIteratorContents(stack.iterator(), "one", "two", "three");
    assertIteratorContents(stack.listIterator(), "one", "two", "three");
    assertIteratorContents(stack.iteratorReverse(), "three", "two", "one");
    assertEquals("one", stack.head());
    assertEquals("three", stack.tail());

    // Verify the java.util implementation
    final List<String> list = new ArrayList<String>();
    list.add("one");
    list.add("two");
    listItr = list.listIterator();
    assertIteratorContents(listItr, "one", "two");
    listItr.add("three");
    assertIteratorContents(list.listIterator(), "one", "two", "three");

  }

  @Test
  public void testListIterator_AddToBeginning() {
    stack.listIterator().add("zero");
    assertIteratorContents(stack.iterator(), "zero", "one", "two");
    assertIteratorContents(stack.listIterator(), "zero", "one", "two");
    assertIteratorContents(stack.iteratorReverse(), "two", "one", "zero");
    assertEquals("zero", stack.head());
    assertEquals("two", stack.tail());
  }

  @Test
  public void testListIterator_ManyOperations() {
    ListIterator<String> listItr = stack.listIterator();
    listItr.next();
    listItr.add("one.five");
    listItr.add("one.seven.five");
    listItr.previous();
    listItr.previous();
    listItr.add("one.two.five");
    listItr.next();
    listItr.previous();
    listItr.previous();
    listItr.previous();
    listItr.previous();
    assertIteratorContents(listItr, "one", "one.two.five", "one.five",
      "one.seven.five", "two");
    assertIteratorContents(stack.iterator(), "one", "one.two.five", "one.five",
      "one.seven.five", "two");
    assertIteratorContents(stack.listIterator(), "one", "one.two.five",
      "one.five", "one.seven.five", "two");
    assertIteratorContents(stack.iteratorReverse(), "two", "one.seven.five",
      "one.five", "one.two.five", "one");
    assertEquals("one", stack.head());
    assertEquals("two", stack.tail());

    // Verify the java.util implementation
    final List<String> list = new ArrayList<String>();
    list.add("one");
    list.add("two");
    listItr = list.listIterator();
    listItr.next();
    listItr.add("one.five");
    listItr.add("one.seven.five");
    listItr.previous();
    listItr.previous();
    listItr.add("one.two.five");
    listItr.previous();
    listItr.previous();
    assertIteratorContents(listItr, "one", "one.two.five", "one.five",
      "one.seven.five", "two");
    assertIteratorContents(list.iterator(), "one", "one.two.five", "one.five",
      "one.seven.five", "two");
  }

  @Test
  public void testListIterator_Remove() {
    stack.add("three");
    final ListIterator<String> listItr = stack.listIterator();
    listItr.next();
    listItr.remove();
    listItr.next();
    assertIteratorContents(stack.iterator(), "one", "three");
    assertIteratorContents(stack.listIterator(), "one", "three");
    assertIteratorContents(stack.iteratorReverse(), "three", "one");
    assertEquals("one", stack.head());
    assertEquals("three", stack.tail());
  }

  @Test
  public void testListIterator_RemoveAtBeginning() {
    stack.add("three");
    final ListIterator<String> listItr = stack.listIterator();
    listItr.remove();
    assertIteratorContents(stack.iterator(), "two", "three");
    assertIteratorContents(stack.listIterator(), "two", "three");
    assertIteratorContents(stack.iteratorReverse(), "three", "two");
    assertEquals("two", stack.head());
    assertEquals("three", stack.tail());
  }

  @Test
  public void testListIterator_RemoveAtEnd() {
    stack.add("three");
    final ListIterator<String> listItr = stack.listIterator();
    listItr.next();
    listItr.next();
    listItr.remove();
    assertIteratorContents(stack.iterator(), "one", "two");
    assertIteratorContents(stack.listIterator(), "one", "two");
    assertIteratorContents(stack.iteratorReverse(), "two", "one");
    assertEquals("one", stack.head());
    assertEquals("two", stack.tail());
  }

  @Test
  public void testReverseForEach() {
    final String[] expected = new String[] {
        "two"
    };
    final Iterator<String> reverse = stack.iteratorReverse();
    while (reverse.hasNext()) {
      final String s = reverse.next();
      assertEquals(expected[0], s);
      expected[0] = "one";
    }

  }

  @Test
  public void testReverseIterate() {
    assertIteratorContents(stack.iteratorReverse(), "two", "one");
  }

  @Override
  protected SimpleLinkedList<String> newList() {
    return new SimpleLinkedList<String>();
  }

}

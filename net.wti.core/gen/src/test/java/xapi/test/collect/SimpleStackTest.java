/**
 *
 */
package xapi.test.collect;

import xapi.collect.impl.SimpleStack;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class SimpleStackTest extends
  AbstractLinkedListTest<SimpleStack<String>> {

  @Override
  protected SimpleStack<String> newList() {
    return new SimpleStack<String>();
  }
}

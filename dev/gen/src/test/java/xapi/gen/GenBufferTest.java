package xapi.gen;

import org.junit.Test;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public class GenBufferTest {


    class Child extends NodeTypeList<TestBufferParent, TestBufferSelf, TestBufferChild> {

    }

  @Test
  public void testPrefixAppend() {
    final TestBufferAncestor ancestor = new TestBufferAncestor();
    final TestBufferParent parent1 = ancestor.createParent();
  }
}

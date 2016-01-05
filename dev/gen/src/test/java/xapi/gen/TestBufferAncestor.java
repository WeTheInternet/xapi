package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public class TestBufferAncestor implements TestBufferTypeWithChildren<TestBufferAncestor, TestBufferAncestor, TestBufferParent> {

  @Override
  public TestBufferAncestor parent() {
    return this; // nasty trick.  You can return null if you want to null check
  }

  @Override
  public TestBufferAncestor ancestor() {
    return this;
  }

  public TestBufferParent createParent() {
    TestBufferParent parent = new TestBufferParent(this);
    ctx(TestBufferParent.class).addChild(parent);
    return parent;
  }
}

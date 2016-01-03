package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public class TestBufferAncestor implements TestBufferType<TestBufferAncestor, TestBufferAncestor, TestBufferParent> {

  private static interface Root extends GenBuffer<Root, TestBufferAncestor> {}
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
    final NodeWithParent<TestBufferAncestor, TestBufferAncestor> ctx = ctx();
    ctx.addChild(parent);
    return parent;
  }
}

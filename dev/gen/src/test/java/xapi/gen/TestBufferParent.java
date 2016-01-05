package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public class TestBufferParent implements TestBufferTypeWithChildren<TestBufferAncestor, TestBufferParent, TestBufferSelf> {

  private final TestBufferAncestor ancestor;

  public TestBufferParent(TestBufferAncestor ancestor) {
    this.ancestor = ancestor;
  }

  @Override
  public TestBufferAncestor ancestor() {
    return ancestor;
  }

  @Override
  public TestBufferAncestor parent() {
    return ancestor;
  }

  public TestBufferSelf createChild() {
    final TestBufferSelf child = new TestBufferSelf(ancestor, this);
    initChild(child);
    return child;
  }

  protected void initChild(TestBufferSelf child) {}

}

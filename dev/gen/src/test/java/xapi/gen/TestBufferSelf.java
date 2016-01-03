package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public class TestBufferSelf implements TestBufferType<TestBufferParent, TestBufferSelf, TestBufferChild> {
  private final TestBufferAncestor ancestor;
  private final TestBufferParent parent;

  public TestBufferSelf(TestBufferAncestor ancestor, TestBufferParent parent) {
    this.ancestor = ancestor;
    this.parent = parent;
  }

  @Override
  public TestBufferAncestor ancestor() {
    return ancestor;
  }

  @Override
  public TestBufferParent parent() {
    return parent;
  }

  public TestBufferChild1 newChild1() {
    return TestBufferChild1.of(ancestor(), this);
  }

  public TestBufferChild2 newChild2() {
    return TestBufferChild2.of(ancestor(), this);
  }

  public TestBufferChild3 newChild3() {
    return TestBufferChild3.of(ancestor(), this);
  }

  public TestBufferChild1And2 newChild1And2() {
    return TestBufferChild1And2.of(ancestor(), this);
  }

  public TestBufferChild1 startChild1() {
    final TestBufferChild1 child = newChild1();
    addChild(child);
    return child;
  }

  public TestBufferChild2 startChild2() {
    final TestBufferChild2 child = newChild2();
    addChild(child);
    return child;
  }

  protected <C extends TestBufferChild<C>, Child extends GenBuffer<TestBufferSelf, C>> void addChild(C child) {
    final NodeWithParent<TestBufferParent, TestBufferSelf, C> ctx = ctx();
    ctx.addChild(child);
  }

}

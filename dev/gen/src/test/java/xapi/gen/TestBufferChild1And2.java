package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public class TestBufferChild1And2 extends TestBufferAbstract<TestBufferSelf, TestBufferChild1And2> implements TestBufferChild<TestBufferChild1And2>  {

  public TestBufferChild1And2(TestBufferAncestor ancestor, TestBufferSelf testBufferSelf) {
    super(ancestor, testBufferSelf);
  }

  public static TestBufferChild1And2 of(TestBufferAncestor ancestor, TestBufferSelf self) {
    return new TestBufferChild1And2(ancestor, self);
  }
}

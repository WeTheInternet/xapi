package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public class TestBufferChild3 extends TestBufferAbstract<TestBufferSelf, TestBufferChild3> implements TestBufferChild<TestBufferChild3> {

  public TestBufferChild3(TestBufferAncestor ancestor, TestBufferSelf testBufferSelf) {
    super(ancestor, testBufferSelf);
  }

  public static TestBufferChild3 of(TestBufferAncestor ancestor, TestBufferSelf testBufferSelf) {
    return new TestBufferChild3(ancestor, testBufferSelf);
  }
}

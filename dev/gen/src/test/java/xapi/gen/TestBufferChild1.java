package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public interface TestBufferChild1 extends TestBufferChild {

  static TestBufferChild1 of(TestBufferAncestor ancestor, TestBufferSelf parent) {
    return new Child1Only(ancestor, parent);
  }

  class Child1Only extends TestBufferAbstract<TestBufferSelf, TestBufferChild> implements TestBufferChild1 {

    public Child1Only(TestBufferAncestor ancestor, TestBufferSelf testBufferSelf) {
      super(ancestor, testBufferSelf);
    }
  }
}

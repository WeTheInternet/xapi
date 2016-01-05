package xapi.gen;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public interface TestBufferChild2 extends TestBufferChild {

  static TestBufferChild2 of(TestBufferAncestor ancestor, TestBufferSelf parent) {
    return new Child2Only(ancestor, parent);
  }

  class Child2Only extends TestBufferAbstract<TestBufferSelf, TestBufferChild> implements TestBufferChild2 {

    public Child2Only(TestBufferAncestor ancestor, TestBufferSelf testBufferSelf) {
      super(ancestor, testBufferSelf);
    }
  }

}

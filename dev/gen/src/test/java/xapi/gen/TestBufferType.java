package xapi.gen;

import xapi.fu.Out1;
import xapi.gen.NodeWithParentWithChildren.ChildStack;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public interface TestBufferType <
    Parent extends TestBufferType<? extends GenBuffer, Parent, Self>,
    Self extends TestBufferType<Parent, Self, Child>,
    Child extends TestBufferType<Self, Child, ?>
    >  extends GenBuffer<Parent, Self> {

  TestBufferAncestor ancestor();

  @Override
  default NodeWithParentWithChildren<Parent, Self, Child, ChildStack<Child>> ctx() {
    return new NodeWithParentWithChildren<Parent, Self, Child, ChildStack<Child>>() {
      @Override
      protected ChildStack<Child> newStack(Child child) {
        return new ChildStack<>(Out1.immutable1(child));
      }
    };
  }

}

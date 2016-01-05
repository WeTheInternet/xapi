package xapi.gen;

import xapi.fu.Out1;
import xapi.gen.NodeWithParentWithChildren.ChildStack;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public interface TestBufferTypeWithChildren<
    Parent extends TestBufferType<? extends GenBuffer, Parent>,
    Self extends TestBufferTypeWithChildren<Parent, Self, Child>,
    Child extends TestBufferType<Self, ? extends Child>
    >  extends TestBufferType<Parent, Self> {

  TestBufferAncestor ancestor();

  default <C extends Child> NodeWithParentWithChildren<Parent, Self, Child, ChildStack<C>> ctx(Class<? super C> childType) {
    return new NodeWithParentWithChildren<Parent, Self, Child, ChildStack<C>>() {
      @Override
      protected ChildStack<C> newStack(Child child) {
        return new ChildStack<>(Out1.immutable1((C)child));
      }
    };
  }
}

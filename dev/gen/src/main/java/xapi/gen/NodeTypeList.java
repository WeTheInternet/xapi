package xapi.gen;

import xapi.gen.NodeWithParentWithChildren.ChildStack;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 1/1/16.
 */
public class NodeTypeList<Parent extends GenBuffer<?, Parent>, Self extends GenBuffer<Parent, Self>, TypeWithChildParameters>
{
  /**
   * @param <Child> The type parameter of child you expect; must extend the ChildBase type parameter of this class,
   *               which itself extends GenBuffer&lt;Self, ?>
   * @param cls The correct raw class of the type of elements in the stack we are expected to return.
   * @return
   */
  <Child extends GenBuffer<Self, Child>> ChildStack<Child> getChildren(
      // Yes, the bounds on this parameter are backwards;
      // You are expected to supply the
      Class<? super Child> cls
  ) {
    return null;
  }
}

package xapi.gen;

import xapi.gen.NodeWithParentWithChildren.ChildStack;

/**
 * A generator context which nests children of its own type.
 *
 * The parent type is its own type, as are its children.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/28/15.
 */
public class NodeWithParentNested<
    Node extends GenBuffer<Node, Node>,
    Stack extends ChildStack<Node>
    > extends NodeWithParentWithChildren<Node, Node, Node, Stack> {

  protected NodeWithParentNested(Node node) {
    super(node);
  }

  @Override
  protected Stack newStack(Node node) {
    return null;
  }
}

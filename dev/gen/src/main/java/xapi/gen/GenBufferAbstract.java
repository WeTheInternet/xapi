package xapi.gen;

import xapi.fu.In2Out1;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/27/15.
 */
public class GenBufferAbstract
    <
        Parent extends GenBuffer<?, Parent>,
        Self extends GenBuffer<Parent, Self>
    >
    implements GenBuffer <Parent, Self> {

  private final Parent parent;
  private NodeWithParent<Parent, Self> ctx;

  protected GenBufferAbstract(Parent parent) {
    this.parent = parent;
    ctx = initContext(this::defaultContext, parent);
  }

  protected GenBufferAbstract(Parent parent, In2Out1<Parent, Self, NodeWithParent<Parent, Self>> factory) {
    this.parent = parent;
    ctx = initContext(factory, parent);
  }

  protected NodeWithParent<Parent,Self> initContext(In2Out1<Parent, Self, NodeWithParent<Parent, Self>> factory, Parent parent) {
    final NodeWithParent<Parent, Self> c = factory.io(parent, self());
    return c;
  }

  protected NodeWithParent<Parent,Self> defaultContext(Parent parent, Self self) {
    return new NodeWithParent<>(self);
  }

  @Override
  public NodeWithParent<Parent, Self> ctx() {
    return ctx;
  }

  @Override
  public final Parent parent() {
    return parent;
  }
}

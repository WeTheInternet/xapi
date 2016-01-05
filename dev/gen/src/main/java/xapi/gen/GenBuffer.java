package xapi.gen;

import xapi.fu.In1;
import xapi.fu.Out1;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 12/12/15.
 */
public interface GenBuffer <ParentType extends GenBuffer<?, ParentType>, SelfType extends GenBuffer<ParentType, SelfType>> {

  @SuppressWarnings("unchecked")
  default SelfType self() {
    return (SelfType) this;
  }

  default ParentType parent() {
    assert false : ".parent() not implemented by " + getClass().getName();
    throw new UnsupportedOperationException();
  }

  default NodeWithParent<ParentType, SelfType> ctx() {
    return new NodeWithParent<>(self());
  }

  default SelfType useParent(In1<ParentType> consumer) {
    consumer.in(parent());
    return self();
  }

  default SelfType useSelf(In1<SelfType> consumer) {
    consumer.in(self());
    return self();
  }

  default Out1<SelfType> selfSupplier() {
    return this::self;
  }

  default Out1<ParentType> parentSupplier() {
    return this::parent;
  }

}

package xapi.gen;

import xapi.fu.Out1;
import xapi.gen.NodeWithParentWithChildren.ChildStack;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 1/1/16.
 */
public class GenBufferWithManyChildren {

  static class MappedStack <V> extends ChildStack <V> {

    public MappedStack(Out1<V> value) {
      super(value);
    }
  }
}

package xapi.fu;

import xapi.fu.In1.In1Unsafe;
import xapi.fu.Out1.Out1Unsafe;

import static xapi.fu.Immutable.immutable1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/18/16.
 */
public class Mutable <I> implements In1Unsafe, Out1Unsafe{

  private volatile I i;

  public Mutable() {
  }

  public Mutable(I value) {
    i = value;
  }

  public Immutable<I> freeze() {
    return immutable1(i);
  }

  @Override
  public void inUnsafe(Object in) throws Throwable {

  }

  @Override
  public Object outUnsafe() throws Throwable {
    return null;
  }
}

package xapi.util.api;

import xapi.annotation.inject.InstanceDefault;


/**
 * This is not "actually" a pointer, but a hard Reference.
 * Since that name is taken, we go with Pointer. :)
 * <p>
 * It implements {@link ProvidesValue}, {@link ReceivesValue} and {@link Bean}.
 * <p>
 * This class is final for compiler optimization;
 * you should pass references as Bean if you want api compatibility,
 * and Pointer if you want better inlining characteristics.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 * @param <X> - The type of X this pointer wraps.
 */
@InstanceDefault(implFor=Bean.class)
public final class Pointer<X>
implements ProvidesValue<X>, ReceivesValue<X>, Bean<X> {

  public Pointer() {
  }

  public Pointer(X x) {
    set(x);
  }

  private volatile X x;

  public final void set(X x) {
    this.x=x;
  }

  @Override
  public final X get() {
    return x;
  }

  public final X remove() {
    try {
      return x;
    }finally {
      x = null;
    }
  }
}

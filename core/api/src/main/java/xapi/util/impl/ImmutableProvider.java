package xapi.util.impl;

import xapi.util.api.ProvidesValue;

/**
 * A completely final immutable provider which will compile down to a direct field access.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <X>
 */
public final class ImmutableProvider <X> implements ProvidesValue<X> {
  private final X x;

  public ImmutableProvider(X x) {
    this.x = x;
  }

  @Override
  public final X get() {
    return x;
  }
}
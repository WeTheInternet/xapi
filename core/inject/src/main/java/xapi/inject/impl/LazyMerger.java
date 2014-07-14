package xapi.inject.impl;

import xapi.util.api.MergesValues;

/**
 * A default object designed to convert two values into one value.
 * <br>
 * The {@link #create(K1, K2)} method is provided to initializes the value.
 * If you use a constructor that accepts another {@link MergesValues},
 * that delegate will only be called to create the value object.
 * If you override the create method, it will be called until it returns non null.
 * <br>
 * If you want to perform some initialization on every call to {@link #merge(K1, K2)},
 * you should override the {@link #refresh(K1, K2, V)} method.
 * <br>
 * Care should be taken to only perform idempotent actions inside the refresh method,
 * as implementations are free to assume it is safe (and performant) to call refresh many times.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <K1>
 * @param <K2>
 * @param <V>
 */
public class LazyMerger <K1, K2, V>
implements MergesValues<K1, K2, V>
{

  public LazyMerger() {
  }
  public LazyMerger(MergesValues<K1, K2, V> merger) {
    this.delegate = merger;
  }
  public LazyMerger(MergesValues<K1, K2, V> merger, V value) {
    this.delegate = merger;
    this.value = value;
  }
  public LazyMerger(V value) {
    this.value = value;
  }

  MergesValues<K1, K2, V> delegate;
  V value;

  @Override
  public final V merge(K1 k1, K2 k2) {
    if (value == null) {
      value = create(k1, k2);
      if (value != null) {
        refresh(k1, k2, value);
      }
    } else {
      refresh(k1, k2, value);
    }
    return value;
  }

  /**
   * Default action is to do nothing.
   * <br/>
   * This method is provided so you can fill in actions that should be taken
   * on the value result, (only called when the value is not null).
   */
  protected void refresh(K1 k1, K2 k2, V value) {
  }

  protected V create(K1 k1, K2 k2) {
    return delegate == null ? null :
      delegate.merge(k1, k2);
  }

}

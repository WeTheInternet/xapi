package xapi.util.impl;

import javax.inject.Provider;

import xapi.inject.impl.SingletonProvider;

public class LazyProvider <T> extends SingletonProvider<T> {

  private final Provider<T> provider;

  public LazyProvider(Provider<T> provider) {
    this.provider = provider;
  }
  
  @Override
  protected final T initialValue() {
    return provider.get();
  }
}

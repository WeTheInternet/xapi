package xapi.util.impl;

import javax.inject.Provider;

import xapi.inject.impl.SingletonProvider;

public class LazyProvider <T> extends SingletonProvider<T> {

  private final Provider<T> provider;

  public LazyProvider(Provider<T> provider) {
    this.provider = provider;
  }
  
  public LazyProvider(T maybeNull, Provider<T> provider) {
    this.provider = 
        maybeNull == null 
          ? provider 
          : new ImmutableProvider<>(maybeNull);
  }
  
  @Override
  protected final T initialValue() {
    return provider.get();
  }
}

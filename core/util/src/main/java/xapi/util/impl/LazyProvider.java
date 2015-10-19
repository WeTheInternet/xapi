package xapi.util.impl;

import xapi.inject.impl.SingletonProvider;

import javax.inject.Provider;

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

  public static <T> LazyProvider<T> of(Provider<T> provider) {
    return new LazyProvider<>(provider);
  }

  public static <T> LazyProvider<T> of(T maybeNull, Provider<T> provider) {
    return new LazyProvider<>(maybeNull, provider);
  }
}

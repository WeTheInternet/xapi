package xapi.util.impl;

import javax.inject.Provider;

import xapi.inject.X_Inject;

public class LazyInstance  <T> extends LazyProvider<T>{

  public LazyInstance(final Class<T> cls) {
    super(new Provider<T>() {
      @Override
      public T get() {
        return X_Inject.instance(cls);
      }
    });
  }

}
package xapi.util.impl;

import xapi.inject.X_Inject;

public class LazySingleton <T> extends LazyProvider<T>{

  public LazySingleton(final Class<T> cls) {
    super(X_Inject.singletonLazy(cls));
  }
  
}

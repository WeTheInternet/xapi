package xapi.inject.api;

import javax.inject.Provider;

public interface InjectionService {

  String MANIFEST_NAME = "xapi.inject";

  void preload(Class<?> cls);

  void setInstanceFactory(Class<?> cls, Provider<?> factory);
  void setSingletonFactory(Class<?> cls, Provider<?> factory);

  <T> Provider<T> getInstanceFactory(Class<T> cls);
  <T> Provider<T> getSingletonFactory(Class<T> cls);

  void requireInstance(Class<?> cls);
  void requireSingleton(Class<?> cls);

  void reload(Class<?> cls);

}

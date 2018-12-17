package xapi.inject.spi;

import java.util.Iterator;
import xapi.collect.impl.AbstractInitMap;
import xapi.collect.impl.AbstractMultiInitMap;
import xapi.except.NotYetImplemented;
import xapi.fu.Out1;

public class RuntimeInjectorAbstract implements InjectService {

  protected final class ScopeMap
    extends AbstractMultiInitMap<String, InjectionScope, String> {

    @SuppressWarnings("unchecked") //ALWAYS_NULL is safe; it is erased to <Object, Object>
    public ScopeMap() {
      super(AbstractInitMap.PASS_THRU, AbstractInitMap.ALWAYS_NULL);
    }


    protected class ScopeIterator implements Iterator<InjectionScope> {

      final String[] components;
      int pos;
      public ScopeIterator(String[] components) {
        pos = (this.components = components).length;
      }

      @Override
      public boolean hasNext() {
        return pos>0;
      }
      @Override
      public InjectionScope next() {
        return getValue(components[--pos]);
      }
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }

    @Override
    protected InjectionScope initialize(String key, String params) {
      return new InjectionScope(params, map);
    }
  }

  protected final ScopeMap map = new ScopeMap();


  protected String getManifestLocation() {
    //allow subclasses to create scoped injection environments.
    return InjectService.MANIFEST_NAME;
  }

  @Override
  public void preload(Class<?> location) {
//    if (!location.endsWith(getManifestLocation())) {
//      if (location.endsWith("/")) {
//        location = location + getManifestLocation();
//      } else {
//        location = location + "/" + getManifestLocation();
//      }
//    }

  }

  @Override
  public void setInstanceFactory(Class<?> cls, Out1<?> provider) {

  }

  @Override
  public void setSingletonFactory(Class<?> cls, Out1<?> provider) {

  }

  @Override
  public <T> Out1<T> getInstanceFactory(Class<T> cls) {
    return null;
  }

  @Override
  public <T> Out1<T> getSingletonFactory(Class<T> cls) {
    return null;
  }

  @Override
  public void requireInstance(Class<?> cls) {

  }

  @Override
  public void requireSingleton(Class<?> cls) {

  }

  @Override
  public void reload(Class<?> cls) {
    throw new NotYetImplemented("Runtime hotswapping is not yet implemented");
  }

}

package xapi.jre.reflect;

import java.lang.reflect.Array;

import xapi.annotation.inject.SingletonDefault;
import xapi.reflect.service.ReflectionService;

@SingletonDefault(implFor=ReflectionService.class)
public class JreReflectionService implements ReflectionService{

//  @Override
//  public <T> void async(final Class<T> classLit, final ClassDataCallback<T> callback) {
//    X_Time.runLater(new Runnable() {
//      @Override
//      public void run() {
//        callback.onSuccess(classLit);
//      }
//    });
//  }

  @Override
  public <T> Class<T> magicClass(Class<T> classLit) {
    return classLit;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] newArray(Class<T> classLit, int dimension) {
    return (T[])Array.newInstance(classLit, dimension);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] newArray(Class<T> classLit, int ... dimensions) {
    return (T[])Array.newInstance(classLit, dimensions);
  }

  /**
   * This method is necessary for gwt-dev mode only;
   * For normal JREs, this class is pretty much a no-op.
   */
  @Override
  public Package getPackage(Object o) {
    return o.getClass().getPackage();
  }

}

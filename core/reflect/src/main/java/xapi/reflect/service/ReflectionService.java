package xapi.reflect.service;

import java.lang.reflect.Method;

/**
 * A service interface for providing reflection support to limited runtimes.
 *
 * Currently, this serves primarily to allow GWT to use reflection,
 * but there's no reason it cannot be used for gwt-flash or java2objc tranpiling either.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface ReflectionService {

  <T> Class<T> magicClass(Class<T> classLit);

  <T> T[] newArray(Class<T> classLit, int dimension);

  <T> T[][] newArray(Class<T> classLit, int dim1, int dim2);

  <T> T[][][] newArray(Class<T> classLit, int dim1, int dim2, int dim3);

  /**
   * This method is mostly for gwt; but is also useful for web apps and codegen
   *
   * Gwt dev mode has problems as its isolated classloader doesn't provide packages.
   * We can get class metadata directly using the thread classloader,
   * but the instances will throw class cast when trying to use constructors.
   * Classes returned by magicClass in devmode will be fully functional in terms
   * of reflection, but some data, like package, are stripped.
   *
   * It can also be used in gwt-prod to load class data that you do not want
   * compiled into the javascript; it will request the binary class data,
   * and parse out the metadata
   *
   * It can also be used in multi-classloader web app environments,
   * to perform class metadata introspection on objects from foreign classloaders.
   *
   * the extra support needed for gwt-dev is extracted here.
   *
   * @param o
   * @return
   */
  Package getPackage(Object o);

  Package getPackage(String parentName, ClassLoader cl);

  Object invokeDefaultMethod(Method method, Object ... params);
}

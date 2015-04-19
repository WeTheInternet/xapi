/**
 *
 */
package com.google.gwt.reflect.rebind.model;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsNamedType;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * An instance of a GeneratedAnnotation is used to cache two important bits of data we need:
 * <br>
 * 1) The class type of the annotation proxy, so we can verify generated source matches
 * the known source of the generated type name we want to use.
 * <br>
 * 2) Mappings from a given instance of an annotation to a static noArg constructor method.
 * There will be one constructor for every used configuration of the given annotation type.
 * <p>
 * We cache these objects statically during the UnifyAst phase of the gwt compile;
 * This allows us to detect when we have already generated a proxy type for a given
 * annotation class and reuse the existing implementation.  Currently, a new implementation will
 * be generated per recompile, however, we may attempt to leverage incremental compilation type reuse.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class GeneratedAnnotation {
  public GeneratedAnnotation(final Annotation anno, final String proxyPackage, final String proxySimpleName) {
    this.anno = anno;
    this.proxyPackage = proxyPackage;
    this.proxySimpleName = proxySimpleName;
  }
  final String proxyPackage;
  final String proxySimpleName;
  final Annotation anno;

  /**
   * A map from a configured instance of an annotation to a public static factory method.
   * <p>
   * These methods will be generated ad-hoc, and reused instead of regenerated.
   * <p>
   * They will also be placed into new classes, so we don't accidentally
   * reuse a method that will have to load an unrelated class full of dependencies.
   */
  final Map<Annotation, IsNamedType> knownInstances = new HashMap<Annotation, IsNamedType>();

  /**
   * The actual source type of the annotation proxy.
   * <p>
   * Whenever an annotation class is first seen, we will have to generate the source
   * for its proxy class (once per gwt compile).  Then, when we ask gwt for the
   * PrintWriter to save this class, it may return null because a class w/ that name
   * already exists.  So, then we would try to look that type up in the TypeOracle,
   * and then use it's .toSource() method to check what we have generated.
   * <p>
   * If our new source is different, the annotation has changed across compiles
   * (common for super dev mode), so we must update our proxy class.
   */
  JClassType proxy;

  public String getAnnoName() {
    return anno.annotationType().getCanonicalName();
  }

  /**
   * @return true if the map of known instances already contains a key for this annotation.
   */
  public boolean hasProviderMethod(final Annotation annotation) {
    return knownInstances.containsKey(annotation);

  }

  /**
   * @return the fully qualified method name that returns the proxy for the supplied annotation
   */
  public IsNamedType getProviderMethod(final Annotation anno) {
    return knownInstances.get(anno);
  }

  /**
   * @return a mangled method name for the supplied annotation.
   */
  public String getMethodName(final Annotation anno) {
    return anno.annotationType().getCanonicalName().replace('.', '_') + knownInstances.size();
  }

  /**
   * Set the fully qualified method name that returns the given proxy instance
   */
  public void addProviderMethod(final Annotation anno, final IsNamedType type) {
    knownInstances.put(anno, type);
  }

  public void setProxyClassType(final JClassType exists) {
    this.proxy = exists;
  }

  public String getProxyName() {
    return ReflectionUtilJava.qualifiedName(proxyPackage, proxySimpleName);
  }

  public String getProxyPackage() {
    return proxyPackage;
  }

  public String getProxySimpleName() {
    return proxySimpleName;
  }

}
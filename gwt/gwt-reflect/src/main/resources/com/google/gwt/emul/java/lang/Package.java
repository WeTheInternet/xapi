package java.lang;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.security.CodeSource;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;


public class Package implements java.lang.reflect.AnnotatedElement {
  /**
   * Return the name of this package.
   *
   * @return The fully-qualified name of this package as defined in the
   * <em>Java Language Specification, Third Edition</em> <a
   * href="http://java.sun.com/docs/books/jls/third_edition/html/names.html#6.5.3"> &sect;6.5.3</a>, for
   * example, <tt>java.lang</tt>
   */
  public String getName() {
    return pkgName;
  }

  /**
   * Return the title of the specification that this package implements.
   *
   * @return the specification title, null is returned if it is not known.
   */
  public String getSpecificationTitle() {
    return specTitle;
  }

  /**
   * Returns the version number of the specification that this package implements. This version string must be
   * a sequence of positive decimal integers separated by "."'s and may have leading zeros. When version
   * strings are compared the most significant numbers are compared.
   *
   * @return the specification version, null is returned if it is not known.
   */
  public String getSpecificationVersion() {
    return specVersion;
  }

  /**
   * Return the name of the organization, vendor, or company that owns and maintains the specification of the
   * classes that implement this package.
   *
   * @return the specification vendor, null is returned if it is not known.
   */
  public String getSpecificationVendor() {
    return specVendor;
  }

  /**
   * Return the title of this package.
   *
   * @return the title of the implementation, null is returned if it is not known.
   */
  public String getImplementationTitle() {
    return implTitle;
  }

  /**
   * Return the version of this implementation. It consists of any string assigned by the vendor of this
   * implementation and does not have any particular syntax specified or expected by the Java runtime. It may
   * be compared for equality with other package version strings used for this implementation by this vendor
   * for this package.
   *
   * @return the version of the implementation, null is returned if it is not known.
   */
  public String getImplementationVersion() {
    return implVersion;
  }

  /**
   * Returns the name of the organization, vendor or company that provided this implementation.
   *
   * @return the vendor that implemented this package..
   */
  public String getImplementationVendor() {
    return implVendor;
  }

  /**
   * Returns true if this package is sealed.
   *
   * @return true if the package is sealed, false otherwise
   */
  public boolean isSealed() {
    return sealBase != null;
  }

  /**
   * Returns true if this package is sealed with respect to the specified code source url.
   *
   * @param url the code source url
   * @return true if this package is sealed with respect to url
   */
  public boolean isSealed(URL url) {
    return url.equals(sealBase);
  }

  /**
   * Compare this package's specification version with a desired version. It returns true if this packages
   * specification version number is greater than or equal to the desired version number.
   * <p>
   * Version numbers are compared by sequentially comparing corresponding components of the desired and
   * specification strings. Each component is converted as a decimal integer and the values compared. If the
   * specification value is greater than the desired value true is returned. If the value is less false is
   * returned. If the values are equal the period is skipped and the next pair of components is compared.
   *
   * @param desired the version string of the desired version.
   * @return true if this package's version number is greater than or equal to the desired version number
   * @exception NumberFormatException if the desired or current version is not of the correct dotted form.
   */
  public boolean isCompatibleWith(String desired) throws NumberFormatException {
    if (specVersion == null || specVersion.length() < 1) {
      throw new NumberFormatException("Empty version string");
    }

    String[] sa = specVersion.split("\\.", -1);
    int[] si = new int[sa.length];
    for (int i = 0; i < sa.length; i++) {
      si[i] = Integer.parseInt(sa[i]);
      if (si[i] < 0) throw NumberFormatException.forInputString("" + si[i]);
    }

    String[] da = desired.split("\\.", -1);
    int[] di = new int[da.length];
    for (int i = 0; i < da.length; i++) {
      di[i] = Integer.parseInt(da[i]);
      if (di[i] < 0) throw NumberFormatException.forInputString("" + di[i]);
    }

    int len = Math.max(di.length, si.length);
    for (int i = 0; i < len; i++) {
      int d = (i < di.length ? di[i] : 0);
      int s = (i < si.length ? si[i] : 0);
      if (s < d) return false;
      if (s > d) return true;
    }
    return true;
  }

  // /**
  // * Find a package by name in the callers <code>ClassLoader</code> instance.
  // * The callers <code>ClassLoader</code> instance is used to find the package
  // * instance corresponding to the named class. If the callers
  // * <code>ClassLoader</code> instance is null then the set of packages loaded
  // * by the system <code>ClassLoader</code> instance is searched to find the
  // * named package. <p>
  // *
  // * Packages have attributes for versions and specifications only if the class
  // * loader created the package instance with the appropriate attributes. Typically,
  // * those attributes are defined in the manifests that accompany the classes.
  // *
  // * @param name a package name, for example, java.lang.
  // * @return the package of the requested name. It may be null if no package
  // * information is available from the archive or codebase.
  // */
  // public static Package getPackage(String name) {
  // ClassLoader l = ClassLoader.getCallerClassLoader();
  // if (l != null) {
  // return l.getPackage(name);
  // } else {
  // return getSystemPackage(name);
  // }
  // }

  private static final JavaScriptObject packages = JavaScriptObject.createObject();

  /**
   * Get all the packages currently known for the caller's <code>ClassLoader</code> instance. Those packages
   * correspond to classes loaded via or accessible by name to that <code>ClassLoader</code> instance. If the
   * caller's <code>ClassLoader</code> instance is the bootstrap <code>ClassLoader</code> instance, which may
   * be represented by <code>null</code> in some implementations, only packages corresponding to classes
   * loaded by the bootstrap <code>ClassLoader</code> instance will be returned.
   *
   * @return a new array of packages known to the callers <code>ClassLoader</code> instance. An zero length
   * array is returned if none are known.
   */
  public static Package[] getPackages() {
    //uncomment when we get classloader online
//    ClassLoader l = ClassLoader.getCallerClassLoader();
//    if (l != null) {
//      return l.getPackages();
//    } else {
      return getSystemPackages();
//    }
  }

  public static Package getPackage(String pkg) {
    Package pack = get(pkg, packages);
    if (pack == null) {
      pack = new Package(pkg, null, null, null, null, null, null, null, null);
      set(pkg, pack, packages);
    }
    return pack;
  }

  /**
   * Get the package for the specified class. This class must be annotated with @ClassMetadata for the
   * compiler to embed class data correctly.
   *
   * @param class the class to get the package of.
   * @return the package of the class. It may be null if no package information is available from the archive
   * or codebase.
   */
  static Package getPackage(Class c) {
    String name = c.getName();
    int i = name.lastIndexOf('.');
    if (i != -1) {
      name = name.substring(0, i-1);
      // ClassLoader cl = c.getClassLoader();
      // if (cl != null) {
      // return cl.getPackage(name);
      // } else {
      return getSystemPackage(name);
      // }
    } else {
      return null;
    }
  }


  // /**
  // * Get the package for the specified class.
  // * The class's class loader is used to find the package instance
  // * corresponding to the specified class. If the class loader
  // * is the bootstrap class loader, which may be represented by
  // * <code>null</code> in some implementations, then the set of packages
  // * loaded by the bootstrap class loader is searched to find the package.
  // * <p>
  // * Packages have attributes for versions and specifications only
  // * if the class loader created the package
  // * instance with the appropriate attributes. Typically those
  // * attributes are defined in the manifests that accompany
  // * the classes.
  // *
  // * @param class the class to get the package of.
  // * @return the package of the class. It may be null if no package
  // * information is available from the archive or codebase. */
  // static Package getPackage(Class c) {
  // String name = c.getName();
  // int i = name.lastIndexOf('.');
  // if (i != -1) {
  // name = name.substring(0, i);
  // ClassLoader cl = c.getClassLoader();
  // if (cl != null) {
  // return cl.getPackage(name);
  // } else {
  // return getSystemPackage(name);
  // }
  // } else {
  // return null;
  // }
  // }

  /* Return an array of loaded system packages. */
  static Package[] getSystemPackages() {
    Package[] pkgs = new Package[0];
    fill(pkgs, packages);
    return pkgs;
  }
  private static native void fill(Package[] arr, JavaScriptObject pkgs)
  /*-{
     for (var i in pkgs) {
       arr[arr.length] = pkgs[i];
     }
   }-*/;
  private static native Package get(String name, JavaScriptObject pkgs)
  /*-{
       return pkgs[name];
   }-*/;
  private static native void set(String name, Package p, JavaScriptObject pkgs)
  /*-{
       pkgs[name] = p;
   }-*/;
  public static Package getSystemPackage(String name) {
    Package pkg = get(name, packages);
    if (pkg == null) {
      pkg = new Package(name, GWT.getModuleName(), GWT.getPermutationStrongName(),
        null, null, GWT.getModuleName(), null, null, null);
    }
    return pkg;
  }

  /**
   * Return the hash code computed from the package name.
   *
   * @return the hash code computed from the package name.
   */
  @Override
  public int hashCode() {
    return pkgName.hashCode();
  }

  /**
   * Returns the string representation of this Package. Its value is the string "package " and the package
   * name. If the package title is defined it is appended. If the package version is defined it is appended.
   *
   * @return the string representation of the package.
   */
  @Override
  public String toString() {
    String spec = specTitle;
    String ver = specVersion;
    if (spec != null && spec.length() > 0)
      spec = ", " + spec;
    else
      spec = "";
    if (ver != null && ver.length() > 0)
      ver = ", version " + ver;
    else
      ver = "";
    return "package " + pkgName + spec + ver;
  }

  private Class<?> getPackageInfo() {
    if (packageInfo == null) {
      try {
        packageInfo = Class.forName(pkgName + ".package-info", false, loader);
      } catch (ClassNotFoundException ex) {
        // store a proxy for the package info that has no annotations
        class PackageInfoProxy {
        }
        packageInfo = PackageInfoProxy.class;
      }
    }
    return packageInfo;
  }

  /**
   * @throws NullPointerException {@inheritDoc}
   * @since 1.5
   */
  @Override
  public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
    return getPackageInfo().getAnnotation(annotationClass);
  }

  /**
   * @throws NullPointerException {@inheritDoc}
   * @since 1.5
   */
  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return getPackageInfo().isAnnotationPresent(annotationClass);
  }

  /**
   * @since 1.5
   */
  @Override
  public Annotation[] getAnnotations() {
    return getPackageInfo().getAnnotations();
  }

  /**
   * @since 1.5
   */
  @Override
  public Annotation[] getDeclaredAnnotations() {
    return getPackageInfo().getDeclaredAnnotations();
  }

  /**
   * Construct a package instance with the specified version information.
   *
   * @param pkgName the name of the package
   * @param spectitle the title of the specification
   * @param specversion the version of the specification
   * @param specvendor the organization that maintains the specification
   * @param impltitle the title of the implementation
   * @param implversion the version of the implementation
   * @param implvendor the organization that maintains the implementation
   * @return a new package for containing the specified information.
   */
  Package(String name, String spectitle, String specversion, String specvendor, String impltitle,
    String implversion, String implvendor, URL sealbase, ClassLoader loader) {
    pkgName = name;
    implTitle = impltitle;
    implVersion = implversion;
    implVendor = implvendor;
    specTitle = spectitle;
    specVersion = specversion;
    specVendor = specvendor;
    sealBase = sealbase;
    this.loader = loader;
  }

  /* Return an array of loaded system packages. */
  /* Exposing fields which we have elevated from private to protected, for our external class, MagicPackage. */
  protected final String pkgName;
  protected final String specTitle;
  protected final String specVersion;
  protected final String specVendor;
  protected final String implTitle;
  protected final String implVersion;
  protected final String implVendor;
  protected final URL sealBase;
  protected CodeSource codeSource;
  protected transient ClassLoader loader;
  protected transient Class packageInfo;
}

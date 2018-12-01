package xapi.source.api;

import xapi.util.X_Util;

import java.io.Serializable;

public interface HasQualifiedName extends Serializable {

  static boolean isJavaLangObject(HasQualifiedName type) {
    return type.getQualifiedName().equals("java.lang.Object");
  }

  /**
   * @return the package in which this resource lives.
   */
  String getPackage();
  /**
   * @return the class simple name.
   *
   * In versions which operate in class scopes that have a name pool,
   * this will return the simplest name possible; if simple name is taken,
   * then try enclosed name.  If that is missing, the fully qualified name
   * is used instead.
   */
  default String getSimpleName() {
    final String[] name = getEnclosedName().split(".");
    return name[name.length-1];
  }
  /**
   * @return ParentClass.SimpleName
   */
  String getEnclosedName();
  /**
   * @return {@link #getPackage()} + "." + {@link #getEnclosedName()}
   */
  default String getQualifiedName() {
    String pkg = getPackage();
    String enclosed = getEnclosedName();
      return pkg == null || pkg.isEmpty() ? enclosed : enclosed == null || enclosed.isEmpty() ? pkg : enclosed.startsWith(pkg+".") ? enclosed : pkg + "." + enclosed;
  }

  default String getQualifiedComponentName() {
    return getQualifiedName().replaceAll(X_Util.arrayRegex, "");
  }
}

package xapi.source.api;

public interface HasQualifiedName
{
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
  String getSimpleName();
  /**
   * @return ParentClass.SimpleName
   */
  String getEnclosedName();
  /**
   * @return {@link #getPackage()} + "." + {@link #getEnclosedName()}
   */
  String getQualifiedName();
}

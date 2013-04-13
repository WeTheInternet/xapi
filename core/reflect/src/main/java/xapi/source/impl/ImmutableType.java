package xapi.source.impl;

import java.io.Serializable;

import javax.inject.Inject;
import javax.inject.Named;

import xapi.annotation.reflect.KeepConstructor;
import xapi.source.api.IsType;

public final class ImmutableType implements IsType, Serializable{

  private static final long serialVersionUID = 2769305093396292820L;

  private final String pkg;
  private final String simple;
  private final IsType enclosing;

  @Inject
  @KeepConstructor
  public ImmutableType(
    @Named("pkg") String pkg,
    @Named("cls") String simpleName) {
    this.pkg = pkg;
    this.simple = simpleName;
    enclosing = null;
  }

  @Inject
  @KeepConstructor
  public ImmutableType(
    @Named("parent") IsType enclosingType,
    @Named("cls") String simpleName) {
    this.pkg = enclosingType.getPackage();
    this.enclosing = enclosingType;
    this.simple = simpleName;
  }


  @Override
  public String getPackage() {
    return pkg;
  }

  @Override
  public String getSimpleName() {
    return simple;
  }

  @Override
  public String getEnclosedName() {
    if (enclosing == null)
      return getSimpleName();
    if (enclosing.getEnclosedName().length()==0)
      return getSimpleName();
    return enclosing.getEnclosedName()+"."+getSimpleName();
  }

  @Override
  public String getQualifiedName() {
    if (pkg.length()==0 || "java.lang".equals(pkg))return getEnclosedName();
    return getPackage()+"."+getEnclosedName();
  }

  @Override
  public boolean isPrimitive() {
    return (pkg.length()==0||"java.lang".equals(pkg))&&Character.isLowerCase(getSimpleName().charAt(0));
  }

  @Override
  public IsType getEnclosingType() {
    return enclosing;
  }

  @Override
  public int hashCode() {
    int hash = 31*pkg.hashCode()+simple.hashCode();
    return enclosing == null ? hash : enclosing.hashCode() * 31 + hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)return true;
    if (!(obj instanceof IsType))return false;
    IsType type = (IsType)obj;
    return simple.equals(type.getSimpleName()) &&
      pkg.equals(type.getPackage()) &&
      enclosing == null ? type.getEnclosingType() == null :
        enclosing.equals(type.getEnclosingType());
  }

}

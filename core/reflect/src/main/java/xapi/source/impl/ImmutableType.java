package xapi.source.impl;

import xapi.annotation.reflect.KeepConstructor;
import xapi.fu.Maybe;
import xapi.fu.X_Fu;
import xapi.fu.itr.ArrayIterable;
import xapi.source.api.HasTypeParams;
import xapi.source.api.IsType;
import xapi.source.api.IsTypeParameter;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

public final class ImmutableType implements IsType, Serializable{

  private static final long serialVersionUID = 2769305093396292820L;

  private final String pkg;
  private final String simple;
  private final IsType enclosing;
  private final IsTypeParameter[] params;

  @Inject
  @KeepConstructor
  public ImmutableType(
    @Named("pkg") String pkg,
    @Named("cls") String simpleName,
    IsTypeParameter ... params) {
    simpleName = simpleName.replace('$', '.');
    int ind = simpleName.lastIndexOf('.');
    if (ind == -1) {
      this.simple = simpleName;
      this.enclosing = null;
    } else {
      this.simple = simpleName.substring(ind+1);
      this.enclosing = new ImmutableType(pkg, simpleName.substring(0, ind));
    }
    this.pkg = pkg;
    this.params = params;
  }

  @Inject
  @KeepConstructor
  public ImmutableType(
    @Named("parent") IsType enclosingType,
    @Named("cls") String simpleName,
    IsTypeParameter ... params) {
    this.pkg = enclosingType.getPackage();
    this.enclosing = enclosingType;
    this.simple = simpleName;
    this.params = params;
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
    if (pkg.length()==0)return getEnclosedName();
    return getPackage()+"."+getEnclosedName();
  }

  public String getImportName() {
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
  public String toString() {
    return getQualifiedName();
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

  @Override
  public Maybe<HasTypeParams> ifTypeParams() {
    if (X_Fu.isEmpty(params)) {
      return Maybe.not();
    }
    final ArrayIterable<IsTypeParameter> itr = ArrayIterable.iterate(params);
    return Maybe.immutable(()-> itr);
  }

  @Override
  public IsType getRawType() {
    if (X_Fu.isEmpty(params)) {
      return this;
    }
    return new ImmutableType(getPackage(), getEnclosedName());
  }
}

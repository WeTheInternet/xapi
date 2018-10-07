package xapi.source.impl;

import xapi.fu.itr.SizedIterable;
import xapi.source.api.*;

public class IsClassDelegate implements IsClass{

  private final IsClass cls;
  private final int arrayDepth;

  public IsClassDelegate(IsClass cls) {
    this(cls, 0);
  }

  public IsClassDelegate(IsClass cls, int arrayDepth) {
    this.cls = cls;
    this.arrayDepth = arrayDepth;
  }

  @Override
  public boolean isPrimitive() {
    return cls.isPrimitive();
  }

  @Override
  public IsType getEnclosingType() {
    return cls.getEnclosingType();
  }

  @Override
  public IsType getRawType() {
    return new IsClassDelegate((IsClass)cls.getRawType(), arrayDepth);
  }

  @Override
  public String getObjectName() {
    return cls.getObjectName();
  }

  @Override
  public String getPackage() {
    return cls.getPackage();
  }

  @Override
  public String getSimpleName() {
    return cls.getSimpleName() + arrayString();
  }

  private String arrayString() {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < arrayDepth; i++)
      b.append("[]");
    return b.toString();
  }

  @Override
  public String getEnclosedName() {
    return cls.getEnclosedName() + arrayString();
  }

  @Override
  public String getQualifiedName() {
    return cls.getQualifiedName() + arrayString();
  }

  @Override
  public Iterable<IsAnnotation> getAnnotations() {
    return cls.getAnnotations();
  }

  @Override
  public IsAnnotation getAnnotation(String name) {
    return cls.getAnnotation(name);
  }

  @Override
  public String toSignature() {
    return cls.toSignature()+ arrayString();
  }

  @Override
  public boolean isArray() {
    return arrayDepth > 0 || cls.isArray();
  }

  @Override
  public boolean isPublic() {
    return cls.isPublic();
  }

  @Override
  public boolean isPrivate() {
    return cls.isPrivate();
  }

  @Override
  public boolean isProtected() {
    return cls.isProtected();
  }

  @Override
  public boolean isPackageProtected() {
    return cls.isPackageProtected();
  }

  @Override
  public boolean hasModifier(int modifier) {
    return cls.hasModifier(modifier);
  }

  @Override
  public Iterable<IsMethod> getDeclaredMethods() {
    return cls.getDeclaredMethods();
  }

  @Override
  public int getModifier() {
    return cls.getModifier();
  }

  @Override
  public Iterable<IsMethod> getMethods() {
    return cls.getMethods();
  }

  @Override
  public IsMethod getMethod(String name, IsType... params) {
    return cls.getMethod(name, params);
  }

  @Override
  public IsMethod getMethod(String name, boolean checkErased,
      Class<?>... params) {
    return cls.getMethod(name, checkErased, params);
  }

  @Override
  public Iterable<IsField> getFields() {
    return cls.getFields();
  }

  @Override
  public IsField getField(String name) {
    return cls.getField(name);
  }

  @Override
  public SizedIterable<IsTypeParameter> getTypeParams() {
    return cls.getTypeParams();
  }

  @Override
  public IsTypeParameter getTypeParam(String name) {
    return cls.getTypeParam(name);
  }

  @Override
  public boolean hasTypeParams() {
    return cls.hasTypeParams();
  }

  @Override
  public Iterable<IsClass> getInterfaces() {
    return cls.getInterfaces();
  }

  @Override
  public boolean hasInterface() {
    return cls.hasInterface();
  }

  @Override
  public boolean isAbstract() {
    return cls.isAbstract();
  }

  @Override
  public boolean isFinal() {
    return cls.isFinal();
  }

  @Override
  public boolean isStatic() {
    return cls.isStatic();
  }

  @Override
  public boolean isInterface() {
    return cls.isInterface();
  }

  @Override
  public boolean isAnnotation() {
    return cls.isAnnotation();
  }

  @Override
  public boolean isEnum() {
    return cls.isEnum();
  }

  @Override
  public IsMethod getEnclosingMethod() {
    return cls.getEnclosingMethod();
  }

  @Override
  public Iterable<IsClass> getInnerClasses() {
    return cls.getInnerClasses();
  }

  @Override
  public Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
    return loader.loadClass(cls.getQualifiedName());
  }

  @Override
  public String toString() {
    return cls.toString();
  }

  @Override
  public int hashCode() {
    return cls.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return cls.equals(obj);
  }

}

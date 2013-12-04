package xapi.source.impl;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.except.NotYetImplemented;
import xapi.source.X_Modifier;
import xapi.source.api.IsAnnotation;
import xapi.source.api.IsClass;
import xapi.source.api.IsField;
import xapi.source.api.IsGeneric;
import xapi.source.api.IsMethod;
import xapi.source.api.IsType;
import xapi.source.service.SourceService;

public class AbstractClass extends AbstractMember<AbstractClass> implements IsClass{

  IsClass parentClass;
  private boolean isInterface;
  final Fifo<IsClass> interfaces;
  final Fifo<IsGeneric> generics;
  final Fifo<IsClass> exceptions;
  final Fifo<IsClass> innerClasses;
  final Fifo<IsMethod> methods;
  final Fifo<IsField> fields;
  IsMethod enclosing;
  IsMethod defaultValue;

  public AbstractClass(SourceService service, String pkg, String simple, int modifiers) {
    super(service, pkg, simple, modifiers);
    setPackage(pkg);
    innerClasses = new SimpleFifo<IsClass>();
    methods = new SimpleFifo<IsMethod>();
    fields = new SimpleFifo<IsField>();
    interfaces = new SimpleFifo<IsClass>();
    generics = new SimpleFifo<IsGeneric>();
    exceptions = new SimpleFifo<IsClass>();

  }

  @Override
  public boolean isPrimitive() {
    return "java.lang".equals(getPackage()) && Character.isLowerCase(getSimpleName().charAt(0));
  }

  @Override
  public String getPackage() {
    return packageName;
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  @Override
  public String getEnclosedName() {
    return parentClass == null ? getSimpleName()
      : parentClass.getEnclosedName() + classSeparator() + getSimpleName();
  }

  @Override
  public String getQualifiedName() {
    assert parentClass == null || parentClass.getPackage().equals(getPackage())
       : "Packaging error: parent != child.\nParent: "+parentClass.getQualifiedName()
       +"\nChild: "+getPackage()+pathSeparator()+getEnclosedName();
    return getPackage()+pathSeparator()+getEnclosedName();
  }

  @Override
  protected char classSeparator() {
    return '.';
  }

  private char pathSeparator() {
    return '.';
  }
  @Override
  public Iterable<IsAnnotation> getAnnotations() {
    return annotations.forEach();
  }

  @Override
  public Iterable<IsMethod> getDeclaredMethods() {
    return new DeclaredMemberFilter<IsMethod>(getMethods(), this);
  }
  
  @Override
  public Iterable<IsMethod> getMethods() {
    return methods.forEach();
  }

  @Override
  public IsMethod getMethod(String name, IsType ... params) {
    throw new NotYetImplemented("getMethod in AbstractClass not yet implemented");
  }

  @Override
  public IsMethod getMethod(String name, boolean checkErased, Class<?> ... params) {
    throw new NotYetImplemented("getMethod in AbstractClass not yet implemented");
  }

  @Override
  public Iterable<IsField> getFields() {
    return fields.forEach();
  }

  @Override
  public IsField getField(String name) {
    throw new NotYetImplemented("getField in AbstractClass not yet implemented");
  }

  @Override
  public Iterable<IsGeneric> getGenerics() {
    return generics.forEach();
  }

  @Override
  public IsGeneric getGeneric(String name) {
    throw new NotYetImplemented("getGeneric in AbstractClass not yet implemented");
  }

  @Override
  public IsMethod getEnclosingMethod() {
    return enclosing;
  }

  @Override
  public Iterable<IsClass> getInnerClasses() {
    return innerClasses.forEach();
  }

  @Override
  public Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
    return loader.loadClass(getQualifiedName());
  }

  @Override
  public Iterable<IsClass> getInterfaces() {
    return interfaces.forEach();
  }

  @Override
  public boolean hasInterface() {
    return !interfaces.isEmpty();
  }

  @Override
  public boolean hasGenerics() {
    return !generics.isEmpty();
  }


  protected AbstractClass setEnclosingClass(IsClass parentClass) {
    this.parentClass = parentClass;
    setEnclosingType(parentClass);
    return this;
  }

  protected AbstractClass setEnclosingMethod(IsMethod method) {
    this.enclosing = method;
    setEnclosingType(method);
    return this;
  }

  @Override
  protected AbstractClass setPackage(String pkg) {
    packageName = pkg;
    return this;
  }

  @Override
  protected AbstractClass setSimpleName(String simpleName) {
    this.simpleName = simpleName;
    return this;
  }

  @Override
  protected AbstractClass addAnnotations(Iterable<IsAnnotation> annos) {
    for (IsAnnotation anno : annos)
      annotations.give(anno);
    return this;
  }

  protected AbstractClass addMethod(IsMethod method) {
    this.methods.give(method);
    return this;
  }
  protected AbstractClass addMethods(Iterable<IsMethod> methods) {
    for (IsMethod method : methods)
      this.methods.give(method);
    return this;
  }
  protected AbstractClass setMethods(Iterable<IsMethod> methods) {
    this.methods.clear();
    return addMethods(methods);
  }

  protected AbstractClass setFields(Iterable<IsField> fields) {
    this.fields.clear();
    return addFields(fields);
  }
  protected AbstractClass addFields(Iterable<IsField> fields) {
    for (IsField field : fields)
      this.fields.give(field);
    return this;
  }
  protected AbstractClass addField(IsField field) {
    this.fields.give(field);
    return this;
  }

  protected AbstractClass setGenerics(Iterable<IsGeneric> generics) {
    this.generics.clear();
    addGenerics(generics);
    return this;
  }
  protected AbstractClass addGenerics(Iterable<IsGeneric> generics) {
    for (IsGeneric generic : generics) {
      this.generics.give(generic);
    }
    return this;
  }

  protected AbstractClass addInnerClasses(Iterable<IsClass> clses) {
    for (IsClass cls : clses) {
      innerClasses.give(cls);
    }
    return this;
  }

  protected AbstractClass setInnerClasses(Iterable<IsClass> clses) {
    this.innerClasses.clear();
    return addInnerClasses(clses);
  }

  protected AbstractClass addInterface(IsClass iface) {
    interfaces.give(iface);
    return this;
  }

  protected AbstractClass addInterfaces(Iterable<IsClass> ifaces) {
    for (IsClass iface : ifaces)
      interfaces.give(iface);
    return this;
  }

  protected AbstractClass setInterfaces(Iterable<IsClass> ifaces) {
    this.interfaces.clear();
    return addInterfaces(ifaces);
  }

  @Override
  public String toSignature() {
    return
        X_Modifier.classModifiers(getModifier())
      + (hasInterface() ? " interface " : " class ")
      + (hasGenerics() ? "<"+ generics.join(", ") + ">" : "")
      + (parentClass == null ?
          ( // no superclass, maybe print interface extends
            hasInterface() ? "\nextends " + interfaces.join(", ") : ""
          ) : // we have a superclass
            "\nextends "+parentClass.getEnclosedName()
          + (// do we have interfaces?
             hasInterface() ? "\nimplements "+interfaces.join(", ") : ""
            )
        ) // end parentClass == null ?
      ;
  }

  @Override
  public boolean isInterface() {
    return isInterface;
  }
  
  @Override
  public boolean isAnnotation() {
    return hasModifier(X_Modifier.ANNOTATION);
  }
  
  @Override
  public boolean isEnum() {
    return hasModifier(X_Modifier.ENUM);
  }

  protected AbstractClass makeInterface(boolean add) {
    isInterface = add;
    return this;
  }
  
  public boolean isArray() {
    return getSimpleName().contains("[]");
  };

}

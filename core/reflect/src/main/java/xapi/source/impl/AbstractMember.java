package xapi.source.impl;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.source.Modifier;
import xapi.source.api.HasModifier;
import xapi.source.api.IsAnnotation;
import xapi.source.api.IsMember;
import xapi.source.api.IsMethod;
import xapi.source.api.IsType;
import xapi.source.service.SourceService;

public class AbstractMember <Self extends AbstractMember<Self>> implements IsMember{

  IsType enclosingType;
  final Fifo<IsAnnotation> annotations;
  IsMethod defaultValue;
  String packageName;
  String simpleName;
  int mod;
  private final SourceService service;

  public AbstractMember(SourceService service, String pkg, String simple, int modifiers) {
    this.service = service;
    annotations = newFifo();
    setPackage(pkg);
    setSimpleName(simple);
    this.mod = modifiers;
  }

  protected SourceService getService() {
    return service;
  }

  protected <T> Fifo<T> newFifo() {
    return new SimpleFifo<T>();
  }

  @Override
  public boolean isPrimitive() {
    return "java.lang".equals(getPackage()) && Character.isLowerCase(getSimpleName().charAt(0));
  }

  /**
   * This method is final to remind you to always call setEnclosingType();
   */
  @Override
  public final IsType getEnclosingType() {
    return enclosingType;
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
    IsType enclosed = getEnclosingType();
    assert enclosed == null || enclosed.getPackage().equals(getPackage())
      : "Packaging error: parent != child.\nParent: "+enclosed.getQualifiedName()
      +"\nChild: "+getPackage()+packageSeparator()+getSimpleName();

    return enclosed == null ? getSimpleName()
      : enclosed.getEnclosedName() + classSeparator() + getSimpleName();
  }

  @Override
  public String getQualifiedName() {
    return getPackage()+packageSeparator()+getEnclosedName();
  }

  protected char classSeparator() {
    return getService().classSeparator();
  }

  protected char packageSeparator() {
    return getService().packageSeparator();
  }

  @Override
  public Iterable<IsAnnotation> getAnnotations() {
    return annotations.forEach();
  }

  @Override
  public boolean isPublic() {
    return (mod & 7) == Modifier.PUBLIC;
  }

  @Override
  public boolean isPrivate() {
    return (mod & 7) == Modifier.PRIVATE;
  }

  @Override
  public boolean isProtected() {
    return (mod & 7) == Modifier.PROTECTED;
  }

  @Override
  public boolean isPackageProtected() {
    return (mod & 7) == 0;
  }

  public boolean isAbstract() {
    return (mod & Modifier.ABSTRACT) == Modifier.ABSTRACT;
  }

  public boolean isFinal() {
    return (mod & Modifier.FINAL) == Modifier.FINAL;
  }

  public boolean isStatic() {
    return (mod & Modifier.STATIC) == Modifier.STATIC;
  }

  @SuppressWarnings("unchecked")
  protected Self setPackage(String pkg) {
    packageName = pkg;
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self setSimpleName(String simpleName) {
    this.simpleName = simpleName;
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self setEnclosingType(IsType enclosing) {
    this.enclosingType = enclosing;
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self addAnnotations(Iterable<IsAnnotation> annos) {
    for (IsAnnotation anno : annos)
      annotations.give(anno);
    return (Self)this;
  }
  @SuppressWarnings("unchecked")
  protected Self makePublic() {
    mod = (mod & ~7)+Modifier.PUBLIC;
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self makePrivate() {
    mod = (mod & ~7)+Modifier.PRIVATE;
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self makeProtected() {
    mod = (mod & ~7)+Modifier.PROTECTED;
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self makePackageProtected() {
    mod = (mod & ~7);
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self setProtection(HasModifier protect) {
    mod = (mod & ~7) | (protect.getModifier() & 7);
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self makeAbstract(boolean add) {
    if (add)
      mod |= Modifier.ABSTRACT;
    else
      mod &= ~Modifier.ABSTRACT;
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self makeFinal(boolean add) {
    if (add)
      mod |= Modifier.FINAL;
    else
      mod &= ~Modifier.FINAL;
    return (Self)this;
  }

  @SuppressWarnings("unchecked")
  protected Self makeStatic(boolean add) {
    if (add)
      mod |= Modifier.STATIC;
    else
      mod &= ~Modifier.STATIC;
    return (Self)this;
  }


  @Override
  public boolean hasModifier(int modifier) {
    return (mod | modifier) == mod;
  }

  @Override
  public int getModifier() {
    return mod;
  }

  @Override
  public String toSignature() {
    return getQualifiedName();
  }

  @Override
  public String toString() {
    return toSignature();
  }


}

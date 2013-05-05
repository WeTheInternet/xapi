package xapi.bytecode.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import xapi.bytecode.ClassPool;
import xapi.bytecode.CtClass;
import xapi.bytecode.CtClassType;
import xapi.bytecode.CtField;
import xapi.bytecode.CtMethod;
import xapi.bytecode.FieldInfo;
import xapi.bytecode.MethodInfo;
import xapi.bytecode.NotFoundException;
import xapi.bytecode.annotation.Annotation;
import xapi.collect.api.InitMap;
import xapi.collect.impl.InitMapDefault;
import xapi.except.NotImplemented;
import xapi.source.X_Modifier;
import xapi.source.X_Source;
import xapi.source.api.IsAnnotation;
import xapi.source.api.IsClass;
import xapi.source.api.IsField;
import xapi.source.api.IsGeneric;
import xapi.source.api.IsMember;
import xapi.source.api.IsMethod;
import xapi.source.api.IsType;
import xapi.source.impl.ImmutableType;
import xapi.source.service.SourceAdapterService;
import xapi.util.X_Debug;
import xapi.util.api.ConvertsValue;

public class BytecodeAdapterService implements
    SourceAdapterService<String, CtMethod, CtField, Annotation>, ConvertsValue<String, IsClass> {

  ClassPool pool;
  InitMap<String, IsClass> classes = new InitMapDefault<String, IsClass>(
      InitMapDefault.PASS_THRU, this);
  public BytecodeAdapterService() {
    pool = new ClassPool(true);
    for (URL url : X_Source.getUrls(getClassLoader())) {
      try {
        pool.appendClassPath(url.toExternalForm());
      } catch (NotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public IsClass toClass(String binaryName) {
    return classes.get(binaryName);
  }
  
  @Override
  public IsClass convert(String from) {
    URL location = X_Source.classToUrl(from, getClassLoader());
    try {
      return new ClassAdapter(new CtClassType(location.openStream(), pool));
    } catch (IOException e) {
      throw X_Debug.rethrow(e);
    }
  }

  protected ClassLoader getClassLoader() {
    return BytecodeAdapterService.class.getClassLoader();
  }

  @Override
  public IsMethod toMethod(CtMethod type) {
    return new MethodAdapter(type);
  }

  @Override
  public IsField toField(CtField type) {
    return new FieldAdapter(type);
  }

  @Override
  public IsAnnotation toAnnotation(Annotation type) {
    return new AnnotationAdapter(type);
  }

  class AnnotationAdapter implements xapi.source.api.IsAnnotation {

    public AnnotationAdapter(Annotation type) {
      // TODO Auto-generated constructor stub
    }

    @Override
    public boolean isPrimitive() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public IsType getEnclosingType() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getPackage() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getSimpleName() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getEnclosedName() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getQualifiedName() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Iterable<IsMethod> getMethods() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public IsMethod getMethod(String name, IsType... params) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public IsMethod getMethod(String name, Class<?>... params) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object getDefaultValue(IsMethod method) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Object toAnnotation(ClassLoader loader) {
      // TODO Auto-generated method stub
      return null;
    }

  }

  abstract class MemberAdapter implements IsMember {

    int modifier;
    ImmutableType type;

    MemberAdapter(int modifier, String pkg, String enclosedName) {
      this.modifier = modifier;
      this.type = new ImmutableType(pkg, enclosedName);
    }

    @Override
    public boolean isPrimitive() {
      return type.isPrimitive();
    }

    @Override
    public IsType getEnclosingType() {
      return type.getEnclosingType();
    }

    @Override
    public String getPackage() {
      return type.getPackage();
    }

    @Override
    public String getSimpleName() {
      return type.getSimpleName();
    }

    @Override
    public String getEnclosedName() {
      return type.getEnclosedName();
    }

    @Override
    public String getQualifiedName() {
      return type.getQualifiedName();
    }

    @Override
    public Iterable<xapi.source.api.IsAnnotation> getAnnotations() {
      return null;
    }

    @Override
    public boolean isPublic() {
      return X_Modifier.isPublic(modifier);
    }

    @Override
    public boolean isPrivate() {
      return X_Modifier.isPrivate(modifier);
    }

    @Override
    public boolean isProtected() {
      return X_Modifier.isProtected(modifier);
    }

    @Override
    public boolean isPackageProtected() {
      return X_Modifier.isPackage(modifier);
    }

    @Override
    public boolean hasModifier(int modifier) {
      return X_Modifier.contains(this.modifier, modifier);
    }

    @Override
    public int getModifier() {
      return modifier;
    }

  }

  class ClassAdapter extends MemberAdapter implements IsClass {

    private CtClass cls;

    public ClassAdapter(CtClass type) {
      super(type.getModifiers(), type.getPackageName(), type.getEnclosedName());
      this.cls = type;
    }

    @Override
    public Iterable<IsMethod> getMethods() {
      return null;
    }

    @Override
    public IsMethod getMethod(String name, IsType... params) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public IsMethod getMethod(String name, Class<?>... params) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Iterable<IsField> getFields() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public IsField getField(String name) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Iterable<IsGeneric> getGenerics() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public IsGeneric getGeneric(String name) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean hasGenerics() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Iterable<IsClass> getInterfaces() {
      return null;
    }

    @Override
    public boolean hasInterface() {
      try {
        return cls.getInterfaces().length > 0;
      } catch (NotFoundException e) {
        throw X_Debug.rethrow(e);
      }
    }

    @Override
    public boolean isAbstract() {
      return X_Modifier.isAbstract(getModifier());
    }

    @Override
    public boolean isFinal() {
      return X_Modifier.isFinal(getModifier());
    }

    @Override
    public boolean isStatic() {
      return X_Modifier.isStatic(getModifier());
    }

    @Override
    public boolean isInterface() {
      return cls.isInterface();
    }

    @Override
    public IsMethod getEnclosingMethod() {
      try {
        CtMethod enclosed = cls.getEnclosingMethod();
        return enclosed == null ? null : new MethodAdapter(enclosed);
      } catch (NotFoundException e) {
        throw X_Debug.rethrow(e);
      }
    }

    @Override
    public Iterable<IsClass> getInnerClasses() {
      CtClass[] clses;
      try {
        clses = cls.getNestedClasses();
      } catch (NotFoundException e) {
        throw X_Debug.rethrow(e);
      }
      ArrayList<IsClass> asClasses = new ArrayList<IsClass>(clses.length);
      for (CtClass cls : clses) {
        asClasses.add(new ClassAdapter(cls));
      }
      return asClasses;
    }

    @Override
    public Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      throw new NotImplemented("Class loading is not yet implemented");
    }

    @Override
    public String toSignature() {
      return cls.toString();
    }

  }

  class MethodAdapter extends MemberAdapter implements IsMethod {

    private CtMethod method;

    public MethodAdapter(CtMethod type) {
      super(type.getModifiers(), type.getDeclaringClass().getPackageName(),
          type.getDeclaringClass().getEnclosedName());
      this.method = type;
    }

    @Override
    public IsClass getEnclosingType() {
      return toClass(type.getPackage()+"."+type.getEnclosedName().replace('.', '$'));
    }

    @Override
    public boolean isAbstract() {
      return X_Modifier.isAbstract(getModifier());
    }

    @Override
    public boolean isStatic() {
      return X_Modifier.isStatic(getModifier());
    }

    @Override
    public String getName() {
      return method.getName();
    }

    @Override
    public IsType getReturnType() {
      return X_Source.toType(type.getPackage(), type.getEnclosedName());
    }

    @Override
    public IsType[] getParameters() {
      return null;
    }

    @Override
    public IsGeneric[] getGenerics() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public IsType[] getExceptions() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String toSignature() {
      return method.getSignature();
    }

  }

  class FieldAdapter extends MemberAdapter implements IsField {

    private CtField field;

    public FieldAdapter(CtField type) {
      super(type.getModifiers(), type.getDeclaringClass().getPackageName(),
          type.getDeclaringClass().getEnclosedName());
      this.field = type;
    }

    @Override
    public IsClass getEnclosingType() {
      return (IsClass) super.getEnclosingType();
    }

    @Override
    public String getDeclaredName() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isStatic() {
      return X_Modifier.isStatic(getModifier());
    }

    @Override
    public boolean isVolatile() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isTransient() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public String toSignature() {
      // TODO Auto-generated method stub
      return null;
    }

  }
}
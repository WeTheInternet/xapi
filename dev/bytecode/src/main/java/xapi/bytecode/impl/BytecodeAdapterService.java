package xapi.bytecode.impl;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import xapi.bytecode.ClassPool;
import xapi.bytecode.CtClass;
import xapi.bytecode.CtClassType;
import xapi.bytecode.CtField;
import xapi.bytecode.CtMethod;
import xapi.bytecode.NotFoundException;
import xapi.bytecode.annotation.Annotation;
import xapi.bytecode.annotation.AnnotationDefaultAttribute;
import xapi.bytecode.annotation.MemberValue;
import xapi.bytecode.attributes.ExceptionsAttribute;
import xapi.bytecode.attributes.InnerClassesAttribute;
import xapi.bytecode.attributes.SignatureAttribute;
import xapi.collect.api.InitMap;
import xapi.collect.impl.InitMapDefault;
import xapi.except.NotImplemented;
import xapi.except.NotYetImplemented;
import xapi.inject.impl.SingletonProvider;
import xapi.log.X_Log;
import xapi.source.X_Modifier;
import xapi.source.X_Source;
import xapi.source.api.IsAnnotation;
import xapi.source.api.IsAnnotationValue;
import xapi.source.api.IsClass;
import xapi.source.api.IsField;
import xapi.source.api.IsGeneric;
import xapi.source.api.IsMember;
import xapi.source.api.IsMethod;
import xapi.source.api.IsType;
import xapi.source.api.Primitives;
import xapi.source.impl.DeclaredMemberFilter;
import xapi.source.impl.ImmutableType;
import xapi.source.impl.IsClassDelegate;
import xapi.source.service.SourceAdapterService;
import xapi.util.X_Debug;
import xapi.util.X_String;
import xapi.util.api.ConvertsValue;
import xapi.util.api.Pair;

public class BytecodeAdapterService implements
    SourceAdapterService<String, CtMethod, CtField, Annotation> {

  private final ClassPool pool;
  // In order to delay calling getScanUrls() until after all constructors are
  // called,
  // We use a lazy provider to delay the call until the class pool is actually
  // needed.
  protected final SingletonProvider<ClassPool> classPool = new SingletonProvider<ClassPool>() {
    protected ClassPool initialValue() {
      for (URL url : getScanUrls()) {
        try {
          pool.appendClassPath(url.toExternalForm().replace("file:", ""));
        } catch (NotFoundException e) {
          e.printStackTrace();
        }
      }
      return pool;
    };
  };

  InitMap<String, IsClass> classes = new InitMapDefault<String, IsClass>(
      InitMapDefault.PASS_THRU, new 
      ConvertsValue<String, IsClass>(){
        @Override
        public IsClass convert(String classString) {
          Pair<String, Integer> cls = X_Source.extractArrayDepth(classString);
          URL location = X_Source.classToUrl(cls.get0(), getClassLoader());
          IsClass asClass;
          try {
            X_Log.debug("Converting",cls.get0(),"to",location);
            asClass = new ClassAdapter(new CtClassType(location.openStream(),
                classPool.get()));
          } catch (NullPointerException e) {
            if (cls.get0().equals(cls.get0().toLowerCase()))
              asClass = Primitives.valueOf("_"+cls.get0());
            else
              throw X_Debug.rethrow(e);
          } catch (IOException e) {
            X_Log.error("Unable to find "+cls.get0());
            throw X_Debug.rethrow(e);
          }
          if (cls.get1()>0) {
            return new IsClassDelegate(asClass, cls.get1());
          }
          return asClass;
        }
      });

  public BytecodeAdapterService() {
    this(new ClassPool(true));
  }

  public BytecodeAdapterService(ClassPool pool) {
    this.pool = pool;
  }

  protected URL[] getScanUrls() {
    return X_Source.getUrls(getClassLoader());
  }

  @Override
  public IsClass toClass(String binaryName) {
    return classes.get(binaryName);
  }

  protected ClassLoader getClassLoader() {
    return Thread.currentThread().getContextClassLoader();
//        BytecodeAdapterService.class.getClassLoader();
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

    private Annotation anno;
    private SingletonProvider<IsClass> annoClass = new SingletonProvider<IsClass>() {
      protected IsClass initialValue() {
        return toClass(anno.getTypeName());
      };
    };
    private SingletonProvider<IsAnnotation> retentionAnno = new SingletonProvider<IsAnnotation>() {
      protected IsAnnotation initialValue() {
        return annoClass.get().getAnnotation(Retention.class.getName());
      };
    };

    public AnnotationAdapter(Annotation type) {
      this.anno = type;
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isCompile() {
      IsAnnotation retention = retentionAnno.get();
      if (retention == null) {
        return true;
      }
      IsMethod value = retention.getMethod("value");
      IsAnnotationValue val = retention.getValue(value);
      return RetentionPolicy.CLASS == val.getRawValue();
    }
    @Override
    public boolean isRuntime() {
      IsAnnotation retention = retentionAnno.get();
      if (retention == null) {
        return false;
      }
      IsMethod value = retention.getMethod("value");
      IsAnnotationValue val = retention.getValue(value);
      return RetentionPolicy.RUNTIME == val.getRawValue();
    }
    @Override
    public boolean isSource() {
      IsAnnotation retention = retentionAnno.get();
      if (retention == null) {
        return false;
      }
      IsMethod value = retention.getMethod("value");
      IsAnnotationValue val = retention.getValue(value);
      return RetentionPolicy.SOURCE == val.getRawValue();
    }
    
    @Override
    public IsType getEnclosingType() {
      return annoClass.get().getEnclosingType();
    }

    @Override
    public String getPackage() {
      return annoClass.get().getPackage();
    }

    @Override
    public String getSimpleName() {
      return annoClass.get().getSimpleName();
    }

    @Override
    public String getEnclosedName() {
      return annoClass.get().getEnclosedName();
    }

    @Override
    public String getQualifiedName() {
      return annoClass.get().getQualifiedName();
    }

    @Override
    public Iterable<IsMethod> getMethods() {
      return annoClass.get().getMethods();
    }
    
    @Override
    public Iterable<IsMethod> getDeclaredMethods() {
      return annoClass.get().getDeclaredMethods();
    }

    @Override
    public IsMethod getMethod(String name, IsType... params) {
      return annoClass.get().getMethod(name, params);
    }

    @Override
    public IsMethod getMethod(String name, boolean checkErased, Class<?>... params) {
      return annoClass.get().getMethod(name, checkErased, params);
    }

    @Override
    public IsAnnotationValue getDefaultValue(IsMethod method) {
      return method.getDefaultValue();
    }
    
    @Override
    public IsAnnotationValue getValue(IsMethod value) {
      MemberValue val = anno.getMemberValue(value.getName());
      if (val == null)
        return getDefaultValue(value);
      return BytecodeUtil.extractValue(val, BytecodeAdapterService.this, value.getReturnType());
    }

    @Override
    public Object toAnnotation(ClassLoader loader) {
      throw new NotYetImplemented("AnnotationProxy not yet implemented");
    }
    
    @Override
    public String toString() {
      return getQualifiedName();
    }
    
    @Override
    public int hashCode() {
      return getQualifiedName().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof IsAnnotation) {
        return ((IsAnnotation)obj).getQualifiedName().equals(getQualifiedName());
      }
      return false;
    }

  }

  abstract class MemberAdapter implements IsMember {

    int modifier;
    ImmutableType type;

    protected final SingletonProvider<Iterable<IsAnnotation>> annotations = new SingletonProvider<Iterable<IsAnnotation>>() {
      protected Iterable<IsAnnotation> initialValue() {
        Annotation[] annos = getRawAnnotations();
        if (annos == null) {
          annos = new Annotation[0];
        }
        return new MemberIterable<Annotation, IsAnnotation>(annotationBuilder,
            annos);
      };
    };

    MemberAdapter(int modifier, String pkg, String enclosedName) {
      this.modifier = modifier;
      this.type = new ImmutableType(pkg, enclosedName);
    }

    protected abstract Annotation[] getRawAnnotations();

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
      return annotations.get();
    }

    @Override
    public IsAnnotation getAnnotation(String name) {
      if (name.indexOf('.') == -1) {
        for (IsAnnotation anno : getAnnotations()) {
          if (anno.getSimpleName().equals(name))
            return anno;
        }
      } else {
        for (IsAnnotation anno : getAnnotations()) {
          if (anno.getQualifiedName().equals(name))
            return anno;
        }
      }
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
    
    @Override
    public int hashCode() {
      return getQualifiedName().hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof IsType) {
        return ((IsType)obj).getQualifiedName().equals(getQualifiedName());
      }
      return false;
    }
    
    @Override
    public String toString() {
      return getQualifiedName();
    }
  }

  class MemberIterable<From, To> implements Iterable<To> {

    private To[] members;

    @SuppressWarnings("unchecked")
    public MemberIterable(ConvertsValue<From, To> converter, From[] from) {
      this.members = (To[]) new Object[from.length];
      for (int i = 0, m = from.length; i < m; ++i) {
        members[i] = converter.convert(from[i]);
      }
    }

    @Override
    public Iterator<To> iterator() {
      return new Itr();
    }

    private class Itr implements Iterator<To> {
      private int pos = 0;

      @Override
      public boolean hasNext() {
        return pos < members.length;
      }

      @Override
      public To next() {
        return members[pos++];
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    }

  }

  private final ConvertsValue<Annotation, IsAnnotation> annotationBuilder = new ConvertsValue<Annotation, IsAnnotation>() {
    @Override
    public IsAnnotation convert(Annotation from) {
      return new AnnotationAdapter(from);
    }
  };

  private final ConvertsValue<CtMethod, IsMethod> methodBuilder = new ConvertsValue<CtMethod, IsMethod>() {
    @Override
    public IsMethod convert(CtMethod from) {
      return new MethodAdapter(from);
    }
  };

  private final ConvertsValue<CtField, IsField> fieldBuilder = new ConvertsValue<CtField, IsField>() {
    @Override
    public IsField convert(CtField from) {
      return new FieldAdapter(from);
    }
  };

  private final ConvertsValue<CtClass, IsClass> interfaceBuilder = new ConvertsValue<CtClass, IsClass>() {
    @Override
    public IsClass convert(CtClass from) {
      return toClass(from.getName());
    }
  };

  class ClassAdapter extends MemberAdapter implements IsClass {

    private CtClass cls;

    private SingletonProvider<Iterable<IsMethod>> methods = new SingletonProvider<Iterable<IsMethod>>() {
      protected Iterable<IsMethod> initialValue() {
        return new MemberIterable<CtMethod, IsMethod>(methodBuilder,
            cls.getMethods());
      };
    };

    private SingletonProvider<Iterable<IsField>> fields = new SingletonProvider<Iterable<IsField>>() {
      protected Iterable<IsField> initialValue() {
        return new MemberIterable<CtField, IsField>(fieldBuilder,
            cls.getFields());
      };
    };

    private SingletonProvider<Iterable<IsClass>> interfaces = new SingletonProvider<Iterable<IsClass>>() {
      protected Iterable<IsClass> initialValue() {
        try {
          return new MemberIterable<CtClass, IsClass>(interfaceBuilder,
              cls.getInterfaces());
        } catch (NotFoundException e) {
          throw new RuntimeException("Unable to load interfaces for "
              + X_String.join(", ", cls.getClassFile2().getInterfaces()), e);
        }
      };
    };

    private SingletonProvider<Iterable<IsClass>> innerClasses = new SingletonProvider<Iterable<IsClass>>() {
      protected Iterable<IsClass> initialValue() {
        try {
          return new MemberIterable<CtClass, IsClass>(interfaceBuilder,
              cls.getNestedClasses());
        } catch (NotFoundException e) {
          throw new RuntimeException("Unable to load interfaces for "
              + cls.getAttribute(InnerClassesAttribute.tag), e);
        }
      };
    };

    public ClassAdapter(CtClass type) {
      super(type.getModifiers(), type.getPackageName(), type.getEnclosedName());
      this.cls = type;
    }

    @Override
    protected Annotation[] getRawAnnotations() {
      return cls.getClassFile2().getAnnotations();
    }

    @Override
    public Iterable<IsMethod> getDeclaredMethods() {
      return new DeclaredMemberFilter<IsMethod>(getMethods(), this);
    }
    
    @Override
    public Iterable<IsMethod> getMethods() {
      return methods.get();
    }

    @Override
    public IsMethod getMethod(String name, IsType... params) {
      assert name != null;
      for (IsMethod method : getMethods()) {
        if (method.getName().equals(name)) {
          if (X_Source.typesEqual(method.getParameters(), params))
            return method;
        }
      }
      return null;
    }

    @Override
    public IsAnnotation getAnnotation(String name) {
      return super.getAnnotation(name);
    }
    
    @Override
    public IsMethod getMethod(String name, boolean checkErased, Class<?>... params) {
      assert name != null;
      for (IsMethod method : getMethods()) {
        if (method.getName().equals(name)) {
          if (X_Source.typesEqual(method.getParameters(), params))
            return method;
        }
      }
      if (checkErased) {
        // TODO: lookup hierarchy of each param's IsType to see if we can get a match
      }
      return null;
    }

    @Override
    public Iterable<IsField> getFields() {
      return fields.get();
    }

    @Override
    public IsField getField(String name) {
      for (IsField field : getFields()) {
        if (field.getDeclaredName().equals(name))
          return field;
      }
      return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<IsGeneric> getGenerics() {
      SignatureAttribute attr = (SignatureAttribute) cls.getClassFile2()
          .getAttribute(SignatureAttribute.tag);
      if (attr == null)
        return Collections.EMPTY_LIST;
      System.err.println(attr);
      return Collections.EMPTY_LIST;
    }

    @Override
    public IsGeneric getGeneric(String name) {
      for (IsGeneric g : getGenerics()) {
        if (g.genericName().equals(name))
          return g;
      }
      return null;
    }

    @Override
    public boolean hasGenerics() {
      return getGenerics().iterator().hasNext();
    }

    @Override
    public Iterable<IsClass> getInterfaces() {
      return interfaces.get();
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
    public boolean isAnnotation() {
      return X_Modifier.isAnnotation(getModifier());
    }
    
    @Override
    public boolean isArray() {
      return cls.isArray();
    }
    
    @Override
    public boolean isEnum() {
      return X_Modifier.isEnum(getModifier());
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
        if (enclosed == null)
          return null;
        IsClass method = BytecodeAdapterService.this.toClass(enclosed
            .getDeclaringClass().getName());
        return method.getMethod(enclosed.getName(),
            BytecodeAdapterService.this.toTypes(enclosed.getParameterTypes()));
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
      return cls.getName();
    }

    @Override
    public final String toString() {
      return cls.getName();
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
    protected Annotation[] getRawAnnotations() {
      return method.getMethodInfo2().getAnnotations();
    }

    @Override
    public IsClass getEnclosingType() {
      return toClass(type.getPackage() + "."
          + type.getEnclosedName().replace('.', '$'));
    }

    @Override
    public boolean isAbstract() {
      return X_Modifier.isAbstract(getModifier());
    }
    @Override
    public IsAnnotation getAnnotation(String name) {
      // TODO Auto-generated method stub
      return super.getAnnotation(name);
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
      try {
        CtClass returnType = method.getReturnType();
        return X_Source.toType(returnType.getPackageName(),
            returnType.getEnclosedName());
      } catch (NotFoundException e) {
        throw new AssertionError(e);
      }
    }

    @Override
    public IsType[] getParameters() {
      try {
        return toTypes(method.getParameterTypes());
      } catch (NotFoundException e) {
        throw new AssertionError(e);
      }
    }

    @Override
    public IsGeneric[] getGenerics() {
      return null;
    }

    @Override
    public IsType[] getExceptions() {
      try {
        return toTypes(method.getExceptionTypes());
      } catch (NotFoundException e) {
        ExceptionsAttribute exceptions = method.getMethodInfo2()
            .getExceptionsAttribute();
        return X_Source.toTypes(exceptions.getExceptions());
      }
    }
    
    @Override
    public IsAnnotationValue getDefaultValue() {
      AnnotationDefaultAttribute def = (AnnotationDefaultAttribute)method.getMethodInfo2().getAttribute(AnnotationDefaultAttribute.tag);
      IsType type;
      try {
        type = new ImmutableType(method.getReturnType().getPackageName(), method.getReturnType().getEnclosedName());
      } catch (NotFoundException e) {
        X_Log.warn("Not able to lookup return type for ",method, e);
        type = null;
      }
      return def == null ? null : BytecodeUtil.extractValue(def.getDefaultValue(), BytecodeAdapterService.this, type);
    }

    @Override
    public String toSignature() {
      return method.getSignature();
    }

    @Override
    public String toString() {
      try {
        return method.getReturnType().getName() + " " + method.getLongName();
      } catch (NotFoundException e) {
        throw new AssertionError(e);
      }
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
    protected Annotation[] getRawAnnotations() {
      return field.getFieldInfo2().getAnnotations();
    }

    @Override
    public IsClass getEnclosingType() {
      return (IsClass) super.getEnclosingType();
    }

    @Override
    public String getDeclaredName() {
      return field.getName();
    }

    @Override
    public boolean isStatic() {
      return X_Modifier.isStatic(getModifier());
    }

    @Override
    public boolean isVolatile() {
      return X_Modifier.isVolatile(getModifier());
    }

    @Override
    public boolean isTransient() {
      return X_Modifier.isTransient(getModifier());
    }

    @Override
    public String toSignature() {
      return field.getSignature();
    }

    @Override
    public String toString() {
      return toSignature();
    }

  }

  public IsType[] toTypes(CtClass[] parameterTypes) {
    IsType[] types = new IsType[parameterTypes.length];
    for (int i = 0, m = parameterTypes.length; i < m; ++i) {
      CtClass param = parameterTypes[i];
      types[i] = X_Source.toType(param.getPackageName(),
          param.getEnclosedName());
    }
    return types;
  }
}
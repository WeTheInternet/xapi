package xapi.source.api;

import xapi.fu.itr.SizedIterable;
import xapi.source.util.X_Modifier;

import static xapi.fu.itr.EmptyIterator.none;

public enum Primitives implements IsClass{
  _void {
    @Override
    public final String getSimpleName() {
      return "void";
    }
    @Override
    public final Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      return void.class;
    }
    @Override
    public String getObjectName() {
      return "Void";
    }
  },
  _boolean {
    @Override
    public final String getSimpleName() {
      return "boolean";
    }
    @Override
    public String getObjectName() {
      return "Boolean";
    }
    @Override
    public final Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      return boolean.class;
    }
  },
  _byte {
    @Override
    public final String getSimpleName() {
      return "byte";
    }
    @Override
    public String getObjectName() {
      return "Byte";
    }
    @Override
    public final Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      return byte.class;
    }
  },
  _char {
    @Override
    public final String getSimpleName() {
      return "char";
    }
    @Override
    public String getObjectName() {
      return "Character";
    }
    @Override
    public final Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      return char.class;
    }
  },
  _short {
    @Override
    public final String getSimpleName() {
      return "short";
    }
    @Override
    public String getObjectName() {
      return "Short";
    }
    @Override
    public final Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      return short.class;
    }
  },
  _int {
    @Override
    public final String getSimpleName() {
      return "int";
    }
    @Override
    public String getObjectName() {
      return "Integer";
    }
    @Override
    public final Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      return int.class;
    }
  },
  _long {
    @Override
    public final String getSimpleName() {
      return "long";
    }
    @Override
    public String getObjectName() {
      return "Long";
    }
    @Override
    public final Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      return long.class;
    }
  },
  _float {
    @Override
    public final String getSimpleName() {
      return "float";
    }
    @Override
    public String getObjectName() {
      return "Float";
    }
    @Override
    public final Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      return float.class;
    }
  },
  _double {
    @Override
    public final String getSimpleName() {
      return "double";
    }
    @Override
    public String getObjectName() {
      return "Double";
    }
    @Override
    public final Class<?> toClass(ClassLoader loader) throws ClassNotFoundException {
      return double.class;
    }
  }
  ;

  @Override
  public final boolean isPrimitive() {
    return true;
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public final IsType getEnclosingType() {
    return null;
  }

  @Override
  public final String getPackage() {
    return "";
  }

  @Override
  public final String getEnclosedName() {
    return getSimpleName();
  }

  @Override
  public final String getQualifiedName() {
    return getSimpleName();
  }

  @Override
  public final Iterable<IsAnnotation> getAnnotations() {
    return none();
  }

  @Override
  public final IsAnnotation getAnnotation(String name) {
    return null;
  }

  @Override
  public final String toSignature() {
    return getSimpleName();
  }

  @Override
  public final boolean isPublic() {
    return true;
  }

  @Override
  public final boolean isPrivate() {
    return false;
  }

  @Override
  public final boolean isProtected() {
    return false;
  }

  @Override
  public final boolean isPackageProtected() {
    return false;
  }

  @Override
  public final boolean hasModifier(int modifier) {
    switch (modifier) {
    case X_Modifier.PUBLIC:
    case X_Modifier.STATIC:
    case X_Modifier.FINAL:
      return true;
    default:
      return false;
    }
  }

  @Override
  public IsType getRawType() {
    return this;
  }

  @Override
  public final Iterable<IsMethod> getDeclaredMethods() {
    return none();
  }

  @Override
  public final int getModifier() {
    return X_Modifier.PUBLIC | X_Modifier.STATIC | X_Modifier.FINAL;
  }

  @Override
  public final Iterable<IsMethod> getMethods() {
    return none();
  }

  @Override
  public final IsMethod getMethod(String name, IsType... params) {
    return null;
  }

  @Override
  public final IsMethod getMethod(String name, boolean checkErased,
      Class<?>... params) {
    return null;
  }

  @Override
  public final Iterable<IsField> getFields() {
    return none();
  }

  @Override
  public final IsField getField(String name) {
    return null;
  }

  @Override
  public final SizedIterable<IsTypeParameter> getTypeParams() {
    return none();
  }

  @Override
  public final IsTypeParameter getTypeParam(String name) {
    return null;
  }

  @Override
  public final boolean hasTypeParams() {
    return false;
  }

  @Override
  public final Iterable<IsClass> getInterfaces() {
    return none();
  }

  @Override
  public final boolean hasInterface() {
    return false;
  }

  @Override
  public final boolean isAbstract() {
    return false;
  }

  @Override
  public final boolean isFinal() {
    return true;
  }

  @Override
  public final boolean isStatic() {
    return true;
  }

  @Override
  public final boolean isInterface() {
    return false;
  }

  @Override
  public final boolean isAnnotation() {
    return false;
  }

  @Override
  public final boolean isEnum() {
    return false;
  }

  @Override
  public final IsMethod getEnclosingMethod() {
    return null;
  }

  @Override
  public final Iterable<IsClass> getInnerClasses() {
    return none();
  }

}

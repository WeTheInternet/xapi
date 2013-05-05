package xapi.source.read;

import java.util.Iterator;

import xapi.collect.impl.SimpleStack;

public final class JavaModel {
  private JavaModel() {}
  
  
  public static class IsQualified {
    String qualifiedName;
    String simpleName;
    String packageName;
    
    public IsQualified() {}
    
    public IsQualified(String type) {
      qualifiedName = simpleName = type;
      packageName = "";
    }
    public IsQualified(String pkg, String cls) {
      setType(pkg, cls);
    }
    
    public void setType(String pkg, String cls) {
      simpleName = cls;
      packageName = pkg;
      if (packageName.length() > 0)
        qualifiedName = packageName + "." + cls;
      else
        qualifiedName = cls;
    }

  }
  
  public static class IsType extends IsQualified {
    public IsType() {}
    public IsType(String type) {
      super(JavaLexer.stripTypeMods(type));
    }
    public final SimpleStack<IsGeneric> generics = new SimpleStack<IsGeneric>();
    public int arrayDepth;
  }
  public static class IsNamedType extends IsType {
    public IsNamedType() {}
    public IsNamedType(String name, String type) {
      super(type);
      this.name = name;
    }

    public String name;
  }
  public static class IsVariable extends IsNamedType {
    public String initializer;
    public IsVariable(String name, String type, String initializer) {
      super(name, type);
      this.initializer = initializer;
    }

  }
  public static class IsGeneric extends IsNamedType {
    public IsGeneric(String name, String type) {
      super(name, type);
    }
    public static final int SUPER = -1;
    public static final int CONCRETE = 0;
    public static final int EXTENDS = 1;
    public String name;
    int bounds;
  }
  
  public static class AnnotationMember{
    public AnnotationMember(String name, String initializer) {
      this.name = name;
      if (initializer == null)initializer = "";
      else initializer = initializer.trim();
      this.value = initializer;
      this.isArray = initializer.lastIndexOf('}') != -1;
    }
    String name;
    String value;
    boolean isArray;
    
    @Override
    public String toString() {
      StringBuilder b = new StringBuilder();
      b.append(name);
      if (value.length() > 0) {
        b.append(" = ");
        b.append(value);
      }
      return b.toString();
    }
  }
  public static class IsAnnotation extends IsQualified {

    public IsAnnotation(String type) {
      super(type);
    }

    public final SimpleStack<AnnotationMember> members = new SimpleStack<AnnotationMember>();
    
    public void addMember(AnnotationMember member) {
      assert member != null;
      members.add(member);
    }
    
    public void addMember(String name, String initializer) {
      if (name == null)name = "value";
      name = name.trim();
      if (name.length() == 0) name = "value";
      addMember(new AnnotationMember(name, initializer));
    }
    
    @Override
    public String toString() {
      StringBuilder b = new StringBuilder("@");
      b.append(qualifiedName);
      if (!members.isEmpty()) {
        b.append("(");
        for (Iterator<AnnotationMember> iter = members.iterator(); iter.hasNext(); ) {
          b.append(iter.next());
          if (iter.hasNext())
            b.append(", ");
        }
        b.append(")");
      }
      return b.toString();
    }
  }
  
  public static class HasModifier {
    int modifier;
  }
  public static class HasAnnotations {
    public final SimpleStack<IsAnnotation> annotations = new SimpleStack<IsAnnotation>();
    public void addAnnotation(IsAnnotation annotation) {
      annotations.add(annotation);
    }
    public Iterable<IsAnnotation> getAnnotations() {
      return annotations;
    }
  }
  
  public static class IsParameter {
    
    public IsParameter() {}
    
    public IsParameter(String name, String type) {
      this.type = new IsNamedType(name, type);
    }
    
    HasAnnotations annotations;
    int modifier;
    IsNamedType type;
    public boolean varargs;
    
    public String getType() {
      return type.simpleName;
    }
    
    public String getName() {
      return type.name;
    }
  }
  
  
}

package xapi.source.read;

import xapi.collect.impl.SimpleStack;

import java.lang.reflect.Modifier;
import java.util.Iterator;

public final class JavaModel {
  private JavaModel() {}


  public static class IsQualified {
    String qualifiedName;
    String simpleName;
    String packageName;

    public IsQualified() {}

    public IsQualified(final String type) {
      qualifiedName = simpleName = type;
      packageName = "";
    }
    public IsQualified(final String pkg, final String cls) {
      setType(pkg, cls);
    }

    public String getQualifiedName() {
      return qualifiedName;
    }

    public String getPackage() {
      return packageName;
    }

    public String getSimpleName() {
      return simpleName;
    }

    public void setType(final String pkg, final String cls) {
      simpleName = cls;
      packageName = pkg;
      if (packageName.length() > 0) {
        qualifiedName = packageName + "." + cls;
      } else {
        qualifiedName = cls;
      }
    }

    @Override
    public String toString() {
      return getQualifiedName();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof IsQualified))
        return false;

      final IsQualified that = (IsQualified) o;

      if (qualifiedName != null ? !qualifiedName.equals(that.qualifiedName) : that.qualifiedName != null)
        return false;
      if (simpleName != null ? !simpleName.equals(that.simpleName) : that.simpleName != null)
        return false;
      return packageName != null ? packageName.equals(that.packageName) : that.packageName == null;
    }

    @Override
    public int hashCode() {
      int result = qualifiedName != null ? qualifiedName.hashCode() : 0;
      result = 31 * result + (simpleName != null ? simpleName.hashCode() : 0);
      result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
      return result;
    }
  }

  public static class IsType extends IsQualified {

    public IsType() {
      generics = new SimpleStack<>();
    }

    public IsType(String pkg, String type) {
      super(pkg, type);
      int ind = type.indexOf("[]");
      while (ind > -1) {
        arrayDepth++;
        type = type.replace("[]", "");
        ind = type.indexOf("[]");
      }
      ind = type.indexOf('<');
      if (ind == -1) {
        generics = new SimpleStack<>();
      } else {
        generics = JavaLexer.extractGenerics(type, ind);
      }
    }
    public IsType(String type) {
      super(JavaLexer.stripTypeMods(type));
      int ind = type.indexOf("[]");
      while (ind > -1) {
        arrayDepth++;
        type = type.replace("[]", "");
        ind = type.indexOf("[]");
      }
      ind = type.indexOf('<');
      if (ind == -1) {
        generics = new SimpleStack<>();
      } else {
        generics = JavaLexer.extractGenerics(type, ind);
      }
    }
    public final SimpleStack<IsGeneric> generics;
    public int arrayDepth;

    @Override
    public String toString() {
      final StringBuilder b = new StringBuilder();
      b.append(super.toString());
      if (!generics.isEmpty()) {
        b.append("<");
        for (final IsGeneric generic : generics) {
          b.append(generic.toString());
        }
        b.append(">");
      }
      for (int depth = arrayDepth; depth --> 0; ) {
        b.append("[]");
      }
      return b.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof IsType))
        return false;
      if (!super.equals(o))
        return false;

      final IsType isType = (IsType) o;

      if (arrayDepth != isType.arrayDepth)
        return false;
      if (generics == null || !generics.equals(isType.generics)){
        return false;
      }
      return super.equals(o);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (generics != null ? generics.hashCode() : 0);
      result = 31 * result + arrayDepth;
      result = 31 * result + super.hashCode();
      return result;
    }
  }

  public enum UnitType {
    CLASS("class *"),
    INTERFACE("interface *"),
    ENUM("enum *"),
    ANNOTATION("@interface *"),
    XAPI("@XApi({\"*\"})");

    private final String pattern;

    UnitType(String pattern) {
      this.pattern = pattern;
    }

    public String toDefinition(String name, int protection) {
      String start = "";
      switch (protection) {
        case Modifier.PROTECTED:
          start = "protected ";
          break;
        case Modifier.PUBLIC:
          start = "public ";
          break;
        case Modifier.PRIVATE:
          start = "private ";
          break;
      }
      return start + pattern.replace("*",
          this == XAPI ?
          name
              .replaceAll("\"", "\\\"")
              .replaceAll("\n", "\\n\", \n\"")
              :
          name);
    }
  }

  public static class IsTypeDefinition extends IsType {
    private UnitType unitType;
    private int protection;

    public UnitType getUnitType() {
      return unitType;
    }

    public void setUnitType(UnitType unitType) {
      this.unitType = unitType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof IsTypeDefinition))
        return false;
      if (!super.equals(o))
        return false;

      final IsTypeDefinition that = (IsTypeDefinition) o;

      if (protection != that.protection)
        return false;
      return unitType == that.unitType;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (unitType != null ? unitType.hashCode() : 0);
      result = 31 * result + protection;
      return result;
    }

    public String toDefinition() {
      return unitType.toDefinition(getSimpleName(), protection);
    }

    public int getProtection() {
      return protection;
    }

    public void setProtection(int protection) {
      this.protection = protection;
    }

    public static IsTypeDefinition newInterface(String packageName, String className) {
      final IsTypeDefinition type = newType(packageName, className);
      type.setUnitType(UnitType.INTERFACE);
      return type;
    }

    public static IsTypeDefinition newClass(String packageName, String className) {
      final IsTypeDefinition type = newType(packageName, className);
      type.setUnitType(UnitType.CLASS);
      return type;
    }

    public static IsTypeDefinition newEnum(String packageName, String className) {
      final IsTypeDefinition type = newType(packageName, className);
      type.setUnitType(UnitType.ENUM);
      return type;
    }

    public static IsTypeDefinition newAnnotation(String packageName, String className) {
      final IsTypeDefinition type = newType(packageName, className);
      type.setUnitType(UnitType.ANNOTATION);
      return type;
    }

    private static IsTypeDefinition newType(String packageName, String className) {
      final IsTypeDefinition type = new IsTypeDefinition();
      type.setType(packageName, className);
      type.setProtection(Modifier.PUBLIC);
      return type;
    }
  }

  public static class IsNamedType extends IsType {
    public IsNamedType() {}
    public IsNamedType(final String type, final String name) {
      super(type);
      this.name = name;
    }

    String name;

    public String getName() {
      return name;
    }

    public String typeName() {
      return super.toString();
    }

    public String getQualifiedMemberName() {
      return getQualifiedName() + "." + getName();
    }

    @Override
    public String toString() {
      return typeName()+" "+name;
    }

    public static IsNamedType namedType(String className, String simpleName) {
      return new IsNamedType(className, simpleName);
    }
  }
  public static class IsVariable extends IsNamedType {
    public String initializer;
    public IsVariable(final String type, final String name, final String initializer) {
      super(type, name);
      this.initializer = initializer;
    }

  }
  public static class IsGeneric extends IsNamedType {
    public IsGeneric(final String type, final String name) {
      super(type, name);
    }
    public static final int SUPER = -1;
    public static final int CONCRETE = 0;
    public static final int EXTENDS = 1;
    int bounds;

    @Override
    public String toString() {
      final StringBuilder b = new StringBuilder();
      b.append(name);
      if (bounds != CONCRETE) {
        if (bounds == SUPER) {
          b.append(" super ");
        } else if (bounds == EXTENDS) {
          b.append(" extends ");
        } else {
          assert false : "Generic bounds > 1 and < -1 not allowed. You supplied " + bounds;
        }
      }
      b.append(qualifiedName);
      return b.toString();
    }

  }

  public static class AnnotationMember{
    public AnnotationMember(final String name, String initializer) {
      this.name = name;
      if (initializer == null) {
        initializer = "";
      } else {
        initializer = initializer.trim();
      }
      this.value = initializer;
      this.isArray = initializer.lastIndexOf('}') != -1;
    }
    String name;
    String value;
    boolean isArray;

    @Override
    public String toString() {
      final StringBuilder b = new StringBuilder();
      b.append(name);
      if (value.length() > 0) {
        b.append(" = ");
        b.append(value);
      }
      return b.toString();
    }
  }
  public static class IsAnnotation extends IsQualified {

    public IsAnnotation(final String type) {
      super(type);
    }

    public final SimpleStack<AnnotationMember> members = new SimpleStack<AnnotationMember>();

    public void addMember(final AnnotationMember member) {
      assert member != null;
      members.add(member);
    }

    public void addMember(String name, final String initializer) {
      if (name == null) {
        name = "value";
      }
      name = name.trim();
      if (name.length() == 0) {
        name = "value";
      }
      addMember(new AnnotationMember(name, initializer));
    }

    @Override
    public String toString() {
      final StringBuilder b = new StringBuilder("@");
      b.append(qualifiedName);
      if (!members.isEmpty()) {
        b.append("(");
        for (final Iterator<AnnotationMember> iter = members.iterator(); iter.hasNext(); ) {
          b.append(iter.next());
          if (iter.hasNext()) {
            b.append(", ");
          }
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
    public void addAnnotation(final IsAnnotation annotation) {
      annotations.add(annotation);
    }
    public Iterable<IsAnnotation> getAnnotations() {
      return annotations;
    }

    @Override
    public String toString() {
      return annotations.join(" ");
    }
  }

  public static class IsParameter {

    public IsParameter(final String type, final String name) {
      this.type = new IsNamedType(type, name);
    }

    HasAnnotations annotations;
    int modifier;
    IsNamedType type;
    public boolean varargs;

    public String getType() {
      return type.typeName();
    }

    public String getName() {
      return type.name;
    }

    @Override
    public String toString() {
      final StringBuilder b = new StringBuilder();
      final String modifiers = Modifier.toString(modifier);
      if (modifiers.length() > 0) {
        b.append(modifiers).append(" ");
      }
      if (annotations != null) {
        b.append(annotations);
      }
      b.append(getType());
      if (varargs) {
        b.append(" ... ");
      } else {
        b.append(" ");
      }
      b.append(getName());
      return b.toString();
    }

    public void setVarargs(boolean b) {
      varargs = b;
      if (type.arrayDepth > 0) {
        type.arrayDepth--;
      }
    }
  }


}

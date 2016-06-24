package xapi.dev.source;

import xapi.source.read.JavaLexer;
import xapi.source.read.JavaVisitor.TypeData;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public abstract class MemberBuffer<Self extends MemberBuffer<Self>> extends
    PrintBuffer implements CanAddImports {

  protected final Set<String> annotations;
  protected final Set<String> generics;
  protected final String origIndent;
  protected PrintBuffer javaDoc;

  protected int modifier = Modifier.PUBLIC;

  public MemberBuffer() {
    this("");
  }

  protected MemberBuffer(StringBuilder target) {
    super(target);
    origIndent = indent;
    annotations = new TreeSet<String>();
    generics = new TreeSet<String>();
  }

  public MemberBuffer(final String indent) {
    this.indent = indent;
    origIndent = indent;
    annotations = new TreeSet<String>();
    generics = new TreeSet<String>();
  }

  @SuppressWarnings("unchecked")
  public final Self self() {
    return (Self) this;
  }

  public final Self addGenerics(final String... generics) {
    for (String generic : generics) {
      generic = generic.trim();
      final boolean noImport = generic.contains("!");
      if (noImport) {
        this.generics.add(generic.replace("!", ""));
      } else {
        // pull fqcn into import statements and shorten them
        this.generics.add(getImports().importFullyQualifiedNames(generic));
      }
    }
    return self();
  }

  public abstract String addImport(String cls);

  public abstract String addImport(Class<?> cls);

  public abstract String addImportStatic(String cls);

  public abstract String addImportStatic(Class<?> cls, String name);

  public abstract String addImportStatic(String cls, String name);

  public final Self addImports(final String... clses) {
    for (final String cls : clses) {
      addImport(cls);
    }
    return self();
  }

  public final Self addImports(final Class<?>... clses) {
    for (final Class<?> cls : clses) {
      addImport(cls);
    }
    return self();
  }

  public final Self addAnnotation(final Class<?> anno) {
    this.annotations.add(addImport(anno));
    return self();
  }

  public final Self addAnnotation(String anno) {
    if (anno.charAt(0) == '@') {
      anno = anno.substring(1);
    }
    anno = anno.trim();// never trust user input :)

    final int openParen = anno.indexOf('(');
    if (openParen == -1) {
      final int hasPeriod = anno.lastIndexOf('.');
      if (hasPeriod != -1) {
        // fqcn is the whole string.
        anno = addImport(anno);
      }
    } else {
      // Need to check fqcn for imports
      final int hasPeriod = anno.lastIndexOf('.', openParen);
      if (hasPeriod != -1) {
        // fqcn is the whole string.
        final String annoName = addImport(anno.substring(0, openParen));
        anno = annoName + anno.substring(openParen);
      }

    }
    this.annotations.add(anno);
    return self();
  }

  protected final PrintBuffer createJavadoc() {
    if (javaDoc == null) {
      javaDoc = new PrintBuffer();
    }
    return javaDoc;
  }

  public final Self setJavadoc(final String doc) {
    final String[] bits = doc.split("\n");
    if (bits.length > 0) {
      javaDoc = new PrintBuffer();
      javaDoc.indent = origIndent;
      if (bits.length == 1) {
        javaDoc.println("/** " + doc + " */");
      } else {
        javaDoc.println("/**");
        for (final String bit : bits) {
          javaDoc.print("* ");
          if ("".equals(bit)) {
            javaDoc.println("<br/>");
          } else {
            javaDoc.println(bit);
          }
        }
        javaDoc.println("*/");
      }
    }
    return self();
  }

  public final Self makeFinal() {
    if ((modifier & Modifier.ABSTRACT) > 0)
     {
      modifier &= ~Modifier.ABSTRACT;// "Cannot be both final and abstract";
    }
    modifier = modifier | Modifier.FINAL;
    return self();
  }

  /**
    TODO: StatementBuffers.
    enum StatementType {
    IF, ELSE, ELSE_IF, BINARY, ADD, SUB, MULT, DIV, TRY, CATCH, FINALLY, etc.
    }
    public StatementBuffer makeStatement(StatementType type) {}
   */

  protected Self makeAbstract() {
    if ((modifier & Modifier.FINAL) > 0)
     {
      modifier &= ~Modifier.FINAL;// "Cannot be both final and abstract";
    }
    if ((modifier & Modifier.STATIC) > 0)
     {
      modifier &= ~Modifier.STATIC;// "Cannot be both static and abstract";
    }
    modifier = modifier | Modifier.ABSTRACT;
    return self();
  }

  public final Self makeStatic() {
    if ((modifier & Modifier.ABSTRACT) > 0)
     {
      modifier &= ~Modifier.ABSTRACT; // "Cannot be both static and abstract";
    }
    modifier = modifier | Modifier.STATIC;
    return self();
  }

  public final Self makeConcrete() {
    modifier = modifier & ~Modifier.ABSTRACT;
    return self();
  }

  public final Self makePublic() {
    modifier = (modifier & 0xFFF8) + Modifier.PUBLIC;
    return self();
  }

  public final Self makeProtected() {
    modifier = (modifier & 0xFFF8) + Modifier.PROTECTED;
    return self();
  }

  public final Self makePrivate() {
    modifier = (modifier & 0xFFF8) + Modifier.PRIVATE;
    return self();
  }

  public final Self makePackageProtected() {
    modifier = modifier & 0xFFF8;
    return self();
  }

  public boolean isStatic() {
    return (modifier & Modifier.STATIC) > 0;
  }

  public boolean isFinal() {
    return (modifier & Modifier.FINAL) > 0;
  }

  protected boolean isAbstract() {
    return (modifier & Modifier.ABSTRACT) > 0;
  }

  public Self setModifier(final int modifier) {
    if ((modifier & 7) > 0) {
      switch (modifier & 7) {
      case Modifier.PUBLIC:
        makePublic();
        break;
      case Modifier.PRIVATE:
        makePrivate();
        break;
      case Modifier.PROTECTED:
        makeProtected();
        break;
      }
    }
    this.modifier |= modifier;
    return self();
  }

  @Override
  public final Self append(final boolean b) {
    super.append(b);
    return self();
  }

  @Override
  public final Self append(final char c) {
    super.append(c);
    return self();
  }

  @Override
  public final Self append(final char[] str) {
    super.append(str);
    return self();
  }

  @Override
  public final Self append(final char[] str, final int offset, final int len) {
    super.append(str, offset, len);
    return self();
  }

  @Override
  public final Self append(final CharSequence s) {
    super.append(s);
    return self();
  }

  @Override
  public final Self append(final CharSequence s, final int start, final int end) {
    super.append(s, start, end);
    return self();
  }

  @Override
  public final Self append(final double d) {
    super.append(d);
    return self();
  }

  @Override
  public final Self append(final float f) {
    super.append(f);
    return self();
  }

  @Override
  public final Self append(final int i) {
    super.append(i);
    return self();
  }

  @Override
  public final Self append(final long lng) {
    super.append(lng);
    return self();
  }

  @Override
  public final Self append(final Object obj) {
    super.append(obj);
    return self();
  }

  @Override
  public final Self append(final String str) {
    super.append(str);
    return self();
  }

  @Override
  public final Self indent() {
    super.indent();
    return self();
  }

  @Override
  public final Self indentln(final char[] str) {
    super.indentln(str);
    return self();
  }

  @Override
  public final Self indentln(final CharSequence s) {
    super.indentln(s);
    return self();
  }

  @Override
  public final Self indentln(final Object obj) {
    super.indentln(obj);
    return self();
  }

  @Override
  public final Self indentln(final String str) {
    super.indentln(str);
    return self();
  }

  @Override
  public final Self outdent() {
    super.outdent();
    return self();
  }

  @Override
  public final Self println() {
    super.println();
    return self();
  }

  @Override
  public final Self println(final char[] str) {
    super.println(str);
    return self();
  }

  @Override
  public final Self println(final CharSequence s) {
    super.println(s);
    return self();
  }

  @Override
  public final Self println(final Object obj) {
    super.println(obj);
    return self();
  }

  @Override
  public final Self println(final String str) {
    super.println(str);
    return self();
  }

  @Override
  public final Self printlns(final String str) {
    super.printlns(str);
    return self();
  }

  @Override
  public Self print(final String str) {
    super.print(str);
    return self();
  }

  protected void addNamedTypes(final Set<String> result,
      final Iterable<Entry<String, Class<?>>> types) {
    for (final Entry<String, Class<?>> type : types) {
      final String shortName = addImport(type.getValue());
      result.add(shortName + " " + type.getKey());
    }
  }

  protected void addNamedTypes(final Set<String> result, final String... types) {
    for (String parameter : types) {
      parameter = parameter.trim();
      final int index = parameter.lastIndexOf(' ');
      assert index > 0 : "Malformed named parameter missing ' ': " + parameter
          + "; from " + Arrays.asList(types);
      final TypeData type = JavaLexer.extractType(parameter, 0);
      final String shortName = addImport(type.getImportName());
      result.add(shortName + " " + parameter.substring(index + 1).trim());
    }
  }

  protected void addTypes(final Set<String> result, final Class<?>... types) {
    for (final Class<?> type : types) {
      result.add(addImport(type.getCanonicalName()));
    }
  }

  protected void addTypes(final Set<String> result, final String... types) {
    for (String typeName : types) {
      typeName = typeName.trim();
      final TypeData type = JavaLexer.extractType(typeName, 0);
      if (type.pkgName.length() > 0) {
        final String shortName = addImport(type.getImportName());
        result.add(shortName);
      }
      result.add(typeName);
    }
  }

  @Override
  public String coerce(Object obj) {
    if (obj instanceof Class) {
      return addImport((Class) obj);
    } else if (obj instanceof Enum){
      final Enum e = (Enum) obj;
      // Important: .getClass() will return the enum instance's class, which may actually be
      // a subclass of the enum type, if that enum has a body:
      // enum Type {
      //    Type,
      //    Subtype { void overrides(); }
      //    ;
      //    void overrides(){}
      // }
      String cls = addImport(e.getDeclaringClass());
      return cls+"."+e.name();
    }
    return super.coerce(obj);
  }

  @Override
  public Self makeChild() {
    return (Self) super.makeChild();
  }

  @Override
  public Self add(Object... values) {
    super.add(values);
    return self();
  }
}

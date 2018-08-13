package xapi.dev.source;

import xapi.source.read.JavaLexer;
import xapi.source.read.JavaLexer.TypeDef;
import xapi.source.read.JavaVisitor.TypeData;
import xapi.source.read.SourceUtil;

/**
 * Adapted from {@link FieldBuffer}, with all notion of getters / setters removed...
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class LocalVariable extends MemberBuffer<LocalVariable> implements CanAddImports, VarBuffer<LocalVariable> {

  private final MethodBuffer enclosing;
  private final TypeData fieldType;// The type of the field itself
  private final TypeDef methodType;// The type to expose on methods
  private final String name;
  private final String simpleType;// Shortest form possible for use in source

  private PrintBuffer initializer;
  private boolean exact;

  public LocalVariable(final MethodBuffer enclosingMethod, final String type,
                       final String name) {
    this(enclosingMethod, type, name, INDENT);
  }

  public LocalVariable(final MethodBuffer enclosingMethod, final String type,
                       final String name,
                       final String indent) {
    super(indent, enclosingMethod);
    this.enclosing = enclosingMethod;
    this.name = name;
    this.indent = indent + INDENT;
    // The type to expose on methods; usually == fieldType, unless exposing []
    this.simpleType = enclosing.addImport(type);
    if (type.equals(simpleType) && !type.contains(".")) {
      final String simple = type.split("<")[0];
      enclosing.getImports().tryReserveSimpleName(simple, simple);
    }
    this.methodType = JavaLexer.extractType(simpleType, 0);
    this.fieldType = this.methodType;
  }

  @Override
  public ImportSection getImports() {
    return enclosing.getImports();
  }

  @Override
  public String addImport(final Class<?> cls) {
    return this.enclosing.addImport(cls);
  }

  @Override
  public String addImport(final String cls) {
    return this.enclosing.addImport(cls);
  }

  @Override
  public String addImportStatic(final Class<?> cls, final String name) {
    return this.enclosing.addImportStatic(cls, name);
  }

  @Override
  public String addImportStatic(final String cls) {
    return this.enclosing.addImportStatic(cls);
  }

  @Override
  public String addImportStatic(final String cls, final String name) {
    return this.enclosing.addImportStatic(cls, name);
  }

  public PrintBuffer getInitializer() {
    return getInitializer(true);
  }

  @Override
  public String getName() {
    return name;
  }

  public PrintBuffer getInitializer(boolean newline) {
    if (initializer == null) {
      initializer = new PrintBuffer();
      if (newline) {
        initializer.println().indent();
      }
    }
    return initializer;
  }

  public String getSimpleType() {
    return simpleType;
  }

  @Override
  public Iterable<String> getGenerics() {
    return generics;
  }

  @Override
  public int getModifier() {
    return modifier;
  }

  @Override
  public Iterable<String> getAnnotations() {
    return annotations;
  }

  @Override
  public PrintBuffer getJavaDoc() {
    return javaDoc;
  }

  public LocalVariable setExactName(final boolean exact) {
    this.exact = exact;
    return this;
  }

  public LocalVariable setInitializer(final String initializer) {
    this.initializer = new PrintBuffer();
    this.initializer.print(initializer);
    return this;
  }

  @Override
  public String toSource() {
    if (fieldType == TypeData.NONE) {
      return super.toSource();
    }
    return toVarDefinition() + super.toSource();
  }

}

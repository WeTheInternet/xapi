package xapi.dev.source;

import xapi.source.read.JavaLexer;
import xapi.source.read.JavaLexer.TypeDef;
import xapi.source.read.JavaVisitor.TypeData;

/**
 * Adapted from {@link FieldBuffer}, with all notion of getters / setters removed...
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class LocalVariable extends MemberBuffer<LocalVariable> implements CanAddImports, VarBuffer<LocalVariable> {

  private final MethodBuffer enclosing;
  private TypeData fieldType;// The type of the field itself
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
    this.fieldType = this.methodType.getSimpleName().isEmpty() ? TypeData.NONE : methodType;
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
  public LocalVariable setInitializerPattern(final String initializer, Object replace1, Object ... more) {
    this.initializer = new PrintBuffer();
    this.initializer.pattern(initializer, replace1, more);
    return this;
  }
  public LocalVariable setInitializerPatternLns(final String initializer, Object replace1, Object ... more) {
    this.initializer = new PrintBuffer(getIndentCount() + 1);
    this.initializer.patternlns(initializer, replace1, more);
    return this;
  }

  @Override
  public String toSource() {
    if (fieldType == TypeData.NONE) {
      return super.toSource();
    }
    return toVarDefinition() + super.toSource();
  }

  @Override
  public LocalVariable clear() {
    fieldType = TypeData.NONE;
    super.clear();
    return this;
  }

  public LocalVariable invokeAndAssign(String expr, String assignType, String assignName, boolean reuseExisting) {
    LocalVariable next = enclosing.newVariable(assignType, assignName, reuseExisting);
    next.setInitializer(name + "." + coerceParens(expr));
    return next;
  }

  protected String coerceParens(String expr) {
    final String trimmed = expr.trim();
    return trimmed.endsWith(";") ? expr : trimmed.endsWith(")") ? expr + ";" : expr + "();";
  }

  public void access(String mthd, Object ... more) {
    enclosing.patternln("$1." + mthd, name, more);
  }

  public void accessLns(String mthd, Object ... more) {
    enclosing.patternlns("$1." + mthd, name, more);
  }

  public void invoke(String mthd, Object ... more) {
    enclosing.patternln("$1." + coerceParens(mthd), name, more);
  }

  public void invokeLns(String mthd, Object ... more) {
    enclosing.patternlns("$1." + coerceParens(mthd), name, more);
  }

  public void invokeConstructor(String paramPattern, Object ... more) {
    enclosing.patternln("new $1(" + paramPattern + ")" , simpleType, more);
  }
  public void invokeConstructorLns(String paramPattern, Object ... more) {
    enclosing.patternlns("new $1(" + paramPattern + ")" , simpleType, more);
  }

  public void initConstructor(String paramPattern, Object ... more) {
    setInitializerPattern("new $1(" + paramPattern + ")" , simpleType, more);
  }
  public void initConstructorLns(String paramPattern, Object ... more) {
    setInitializerPatternLns("\nnew $1(" + paramPattern + "\n)" , simpleType, more);
  }
}

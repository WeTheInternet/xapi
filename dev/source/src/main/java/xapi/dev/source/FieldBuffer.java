package xapi.dev.source;

import xapi.source.read.JavaLexer;
import xapi.source.read.JavaLexer.TypeDef;
import xapi.source.read.JavaVisitor.TypeData;
import xapi.source.read.SourceUtil;

/**
 * A field buffer is used to add a field to a generated class.
 *
 * The field definition itself is exported during .toString(), but this buffer
 * also exposes functionality to auto-generate getter, setter, adder, remover
 * and clear methods.
 *
 * The current implementation translates arrays in return types into ArrayList
 * that returns .toArray() copies of elements.
 *
 * This allows you to implement a varargs setter (which clears before add), and
 * varargs adders and removers.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class FieldBuffer extends MemberBuffer<FieldBuffer> implements CanAddImports, VarBuffer<FieldBuffer> {

  private final ClassBuffer cls;
  private final TypeDef type;// The type of the field itself.
  private final String fieldName;
  private final String simpleType;// Shortest form possible for use in source
  private final String methodFragment;// Camel-case form of name, for getField, setField methods.

  private PrintBuffer initializer;
  private MethodBuffer get;
  private MethodBuffer set;
  private MethodBuffer add;
  private MethodBuffer remove;
  private MethodBuffer clear;
  private boolean fluent = true;
  private boolean exact;

  public FieldBuffer(final ClassBuffer enclosingClass, final String type,
    final String name) {
    this(enclosingClass, type, name, INDENT);
  }

  public FieldBuffer(final ClassBuffer enclosingClass, final String type,
    final String name,
    final String indent) {
    super(indent, enclosingClass);
    this.cls = enclosingClass;
    this.fieldName = name;
    this.methodFragment = name.length() == 0 ? "" :
        Character.toUpperCase(name.charAt(0)) + name.substring(1);
    this.indent = indent + INDENT;
    this.simpleType = cls.addImport(type);
    //noinspection StringEquality (we are testing whether addImport returned a different string instance)
    if (type == simpleType && !type.contains(".")) {
      final String simple = type.split("<")[0];
      cls.getImports().tryReserveSimpleName(simple, simple);
    }
    this.type = JavaLexer.extractType(simpleType, 0);
    if (this.type.generics.length() > 0) {
//      addGenerics(this.type.generics);
    }
  }

  public MethodBuffer addAdder() {
    if (add == null) {
      add = initAdder();
    }
    return add;
  }

  public MethodBuffer addClearer() {
    if (clear == null) {
      clear = initClearer();
    }
    return clear;
  }

  public MethodBuffer addGetter(final int modifiers) {
    if (get == null) {
      get = initGetter();
    }
    get.visitModifier(modifiers, cls.context);
    return get;
  }

  public final FieldBuffer createGetter(final int modifiers) {
    addGetter(modifiers);
    return this;
  }

  public final FieldBuffer createSetter(final int modifiers) {
    addSetter(modifiers);
    return this;
  }

  @Override
  public ImportSection getImports() {
    return cls.getImports();
  }

  @Override
  public String addImport(final Class<?> cls) {
    return this.cls.addImport(cls);
  }

  @Override
  public String addImport(final String cls) {
    if (cls.replace(this.cls.getPackage() + ".", "").indexOf('.') == -1) {
      return cls;
    }
    return this.cls.addImport(cls);
  }

  @Override
  public String addImportStatic(final Class<?> cls, final String name) {
    return this.cls.addImportStatic(cls, name);
  }

  @Override
  public String addImportStatic(final String cls) {
    return this.cls.addImportStatic(cls);
  }

  @Override
  public String addImportStatic(final String cls, final String name) {
    return this.cls.addImportStatic(cls, name);
  }

  public MethodBuffer addSetter(final int modifier) {
    if (set == null) {
      set = initSetter();
    }
    set.visitModifier(modifier, cls.context);
    return set;
  }

  public PrintBuffer getInitializer() {
    return getInitializer(true);
  }

  public PrintBuffer getInitializer(boolean newline) {
    if (initializer == null) {
      initializer = new PrintBuffer();
      initializer.indent = indent;
      if (newline) {
        initializer.println().indent();
      }
    }
    return initializer;
  }

  @Override
  public String getName() {
    return fieldName;
  }

  public String getSimpleType() {
    return simpleType;
  }

  public String getRawType() {
    return getSimpleType().split("<")[0];
  }

  public String getType() {
    StringBuilder b = new StringBuilder(simpleType);
    writeGenerics(b);
    return b.toString();
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

  public boolean isFluent() {
    return fluent;
  }

  public FieldBuffer remover() {
    if (remove == null) {
      remove = initRemover();
    }
    return this;
  }

  public FieldBuffer setExactName(final boolean exact) {
    this.exact = exact;
    return this;
  }

  public FieldBuffer setFluent(final boolean fluent) {
    this.fluent = fluent;
    return this;
  }

  public FieldBuffer setInitializer(final String initializer) {
    this.initializer = new PrintBuffer();
    this.initializer.print(initializer);
    return this;
  }

  @Override
  public String toSource() {
    if (type == TypeData.NONE) {
      return super.toSource();
    }
    return toVarDefinition() + super.toSource();
  }

  protected String fluentReturnType() {
    return fluent ? cls.getSimpleName() : "void";
  }

  protected String fluentReturnValue() {
    return fluent ? "this" : "";
  }

  protected String getterName() {
    return exact ? fieldName
      : SourceUtil.toGetterName(type.clsName, methodFragment);
  }

  protected MethodBuffer initAdder() {
    return cls
      .createMethod(
        "public " + fluentReturnType() + " add" + methodFragment + "("
          + simpleType + " " + fieldName + ")")
      .println("this." + fieldName + " = " + fieldName + ";")
      .returnValue(fluentReturnValue());
  };

  protected MethodBuffer initClearer() {
    return cls.createMethod(
      "public " + simpleType + " get" + methodFragment + "()").returnValue(
      fieldName);
  }

  protected TypeData initGenerator(final TypeData originalType) {
    if (originalType.arrayDepth > 0) {
      //
    }
    return originalType;
  }

  protected MethodBuffer initGetter() {
    return cls.createMethod("public " + type + " " + getterName() + "()")
      .returnValue(fieldName);
  }

  protected MethodBuffer initRemover() {
    return cls
      .createMethod(
        "public " + fluentReturnType() + " remove" + methodFragment + "("
          + simpleType + " " + fieldName + ")")
      .println("this." + fieldName + " = null;")
      .returnValue(fluentReturnValue());
  }

  protected MethodBuffer initSetter() {
    final MethodBuffer setter = cls.createMethod(
      "public " + fluentReturnType() + " " + setterName() + "(" + simpleType
        + " " + fieldName + ")").println(
      "this." + fieldName + " = " + fieldName + ";");
    if (isFluent()) {
      setter.returnValue(fluentReturnValue());
    }
    return setter;
  }

  protected String setterName() {
    return exact ? fieldName : "set" + methodFragment;
  }

}

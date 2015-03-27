package com.google.gwt.thirdparty.xapi.dev.source;

import java.lang.reflect.Modifier;

import com.google.gwt.thirdparty.xapi.source.read.JavaLexer;
import com.google.gwt.thirdparty.xapi.source.read.JavaLexer.TypeDef;
import com.google.gwt.thirdparty.xapi.source.read.JavaVisitor.TypeData;

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
public class FieldBuffer extends MemberBuffer<FieldBuffer> {

  private final ClassBuffer cls;
  private final TypeData fieldType;// The type of the field itself
  private final TypeDef methodType;// The type to expose on methods
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
    super(indent);
    this.cls = enclosingClass;
    this.fieldName = name;
    this.methodFragment = Character.toUpperCase(name.charAt(0))
      + (name.length() == 0 ? "" : name.substring(1));
    this.indent = indent + INDENT;
    // The type to expose on methods; usually == fieldType, unless exposing []
    this.simpleType = cls.addImport(type);
    this.methodType = JavaLexer.extractType(simpleType, 0);
    this.fieldType = initGenerator(this.methodType);
  }

  public FieldBuffer addAdder() {
    if (add == null) {
      add = initAdder();
    }
    return this;
  }

  public FieldBuffer addClearer() {
    if (clear == null) {
      clear = initClearer();
    }
    return this;
  }

  public FieldBuffer addGetter(final int modifiers) {
    if (get == null) {
      get = initGetter();
    }
    get.visitModifier(modifiers, cls.context);
    return this;
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

  public FieldBuffer addSetter(final int modifier) {
    if (set == null) {
      set = initSetter();
    }
    set.visitModifier(modifier, cls.context);
    return this;
  }

  public PrintBuffer getInitializer() {
    if (initializer == null) {
      initializer = new PrintBuffer();
      initializer.indent = indent;
      initializer.println().indent();
    }
    return initializer;
  }

  public String getName() {
    return fieldName;
  }

  public String getSimpleType() {
    return simpleType;
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
  public String toString() {
    if (fieldType == TypeData.NONE) {
      return super.toString();
    }

    final StringBuilder b = new StringBuilder(Character.toString(NEW_LINE));
    if (javaDoc != null && javaDoc.isNotEmpty()) {
      b.append(javaDoc.toString());
    }
    b.append(origIndent);
    if (annotations.size() > 0) {
      for (final String anno : annotations) {
        b.append('@').append(anno).append(NEW_LINE + origIndent);
      }
    }
    final String mods = Modifier.toString(modifier);
    if (mods.length() > 0) {
      b.append(mods).append(" ");
    }
    // generics
    if (generics.size() > 0) {
      b.append("<");
      String prefix = "";
      for (final String generic : generics) {
        b.append(prefix);
        b.append(generic);
        prefix = ", ";
      }
      b.append("> ");
    }
    // field type
    b.append(simpleType).append(" ");
    // field name
    b.append(fieldName);
    final String init = initializer == null ? "" : initializer.toString();
    if (init.length() > 0) {
      b.append(" = ").append(init);
      if (!init.trim().endsWith(";")) {
        b.append(";");
      }
    } else {
      b.append(";");
    }
    b.append("\n");
    return b.toString() + super.toString();
  }

  protected String fluentReturnType() {
    return fluent ? cls.getSimpleName() : "void";
  }

  protected String fluentReturnValue() {
    return fluent ? "this" : "";
  }

  protected String getterName() {
    return exact ? fieldName
      : (fieldType.clsName.equalsIgnoreCase("boolean") ? "is" : "get")
        + methodFragment;
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
    return cls.createMethod("public " + methodType + " " + getterName() + "()")
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

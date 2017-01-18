/*
 * Copyright 2013, We The Internet Ltd.
 *
 * All rights reserved.
 *
 * Distributed under a modified BSD License as follow:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistribution in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution, unless otherwise
 * agreed to in a written document signed by a director of We The Internet Ltd.
 *
 * Neither the name of We The Internet nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package xapi.dev.source;

import xapi.collect.impl.SimpleStack;
import xapi.source.read.JavaLexer;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;

public class ClassBuffer extends MemberBuffer<ClassBuffer> {

  public enum FieldOrder {
    StaticTypes,
    InnerTypes,
    StaticFields,
    StaticMethods,
    Fields,
    Constructors,
    Methods
  }

  protected int privacy;
  private boolean isClass;
  private boolean isAnnotation;
  private boolean isEnum;
  private String superClass;
  private String simpleName;
  private final Set<String> interfaces;
  private final SimpleStack<MethodBuffer> ctors;
  protected SourceBuilder<?> context;
  private boolean isWellFormatted;
  private PrintBuffer prefix;
  private final PrintBuffer classes;
  private final PrintBuffer fields;
  private final PrintBuffer constructors;
  private final PrintBuffer methods;
  private PrintBuffer staticFields;
  private PrintBuffer staticMethods;
  private PrintBuffer staticClasses;
  private final PrintBuffer statics;

  private ClassBuffer(ClassBuffer from, StringBuilder target) {
    this(from);
    this.target = target;
  }
  private ClassBuffer(ClassBuffer from) {
    super(from.enclosing);
    // TODO reduce the common classbuffer operations to a supertype "TypeBuffer",
    // where we include all the wiring to add fields and methods, but none of the
    // wrapping where we print things like "public class MyName"
    this.isWellFormatted = false;
    this.prefix = from.prefix;
    this.isClass = from.isClass;
    this.privacy = from.privacy;
    this.isClass = from.isClass;
    this.isAnnotation = from.isAnnotation;
    this.isEnum = from.isEnum;
    this.superClass = from.superClass;
    this.simpleName = from.simpleName;
    this.interfaces = from.interfaces;
    this.ctors = from.ctors;
    this.context = from.context;
    this.classes = from.classes;
    this.fields = from.fields;
    this.methods = from.methods;
    this.constructors = from.constructors;
    this.staticFields = from.staticFields;
    this.staticMethods = from.staticMethods;
    this.staticClasses = from.staticClasses;
    this.statics = from.statics;
  }

  public ClassBuffer() {
    this(new SourceBuilder<>(), null, "");
    context.setBuffer(this);
  }

  public ClassBuffer(final SourceBuilder<?> context, MemberBuffer<?> enclosing, final String indent) {
    super(indent, enclosing);
    indent();
    this.context = context;
    interfaces = new TreeSet<>();
    ctors = new SimpleStack<>();
    classes = new PrintBuffer();
    fields = new PrintBuffer();
    constructors = new PrintBuffer();
    methods = new PrintBuffer();
    statics = new PrintBuffer();
    addToEnd(statics);
    addToEnd(classes);
    addToEnd(fields);
    addToEnd(constructors);
    addToEnd(methods);
  }

  @Override
  public String toSource() {
    final StringBuilder b = new StringBuilder(Character.toString(NEW_LINE));
    if (javaDoc != null && javaDoc.isNotEmpty()) {
      b.append(javaDoc.toString());
    }
    if (annotations.size() > 0) {
      for (final String anno : annotations) {
        b.append(origIndent).append("@").append(anno).append(NEW_LINE);
      }
    }
    b.append(origIndent);
    if (prefix != null) {
      b.append(prefix);
    }
    if (privacy == Modifier.PUBLIC) {
      b.append("public ");
    } else if (privacy == Modifier.PRIVATE) {
      b.append("private ");
    } else if (privacy == Modifier.PROTECTED) {
      b.append("protected ");
    }

    if (isStatic()) {
      b.append("static ");
    }
    if (isAbstract() && !isInterface()) {
      b.append("abstract ");
    }
    if (isFinal()) {
      b.append("final ");
    }

    if (isClass) {
      b.append("class ");
    } else {
      if (isAnnotation) {
        b.append('@');
      }
      if (isEnum) {
        b.append("enum ");
      } else {
        b.append("interface ");
      }
    }

    b.append(simpleName + " ");

    if (generics.size() > 0) {
      b.append("<");
      for (final String generic : generics) {
        b.append(generic);
        b.append(", ");
      }
      b.delete(b.length() - 2, b.length());
      b.append("> ");
    }

    if (isClass) {
      if (superClass != null) {
        b.append("extends " + superClass + " ");
      }
      if (interfaces.size() > 0) {
        b.append("implements ");
        for (final String iface : interfaces) {
          b.append(iface);
          b.append(", ");
        }
        b.delete(b.length() - 2, b.length() - 1);
      }
    } else {
      if (interfaces.size() > 0) {
        if (isEnum) {
          b.append("implements ");
        } else {
          b.append("extends ");
        }
        for (final String iface : interfaces) {
          b.append(iface);
          b.append(", ");
        }
        b.delete(b.length() - 2, b.length());
      }
    }
    b.append("{");
    b.append(NEW_LINE);
    return b + super.toSource();
  }

  public boolean isClass() {
    return isClass;
  }

  public boolean isAnnotation() {
    return isAnnotation;
  }

  public boolean isEnum() {
    return isEnum;
  }

  public boolean isInterface() {
    return !isClass() && !isAnnotation() && !isEnum();
  }

  protected String superString() {
    return super.toSource();
  }

  @Override
  public void addToBeginning(final CharBuffer buffer) {
    if (prefix == null) {
      prefix = new PrintBuffer();
    }
    prefix.addToBeginning(buffer);
  }

  public ClassBuffer setDefinition(String definition, final boolean wellFormatted) {
    final JavaLexer metadata = new JavaLexer(definition);
    isWellFormatted = wellFormatted;
    privacy = metadata.getPrivacy();
    if (metadata.isStatic()) {
      makeStatic();
    }
    if (metadata.isFinal()) {
      makeFinal();
    }
    if (metadata.isAbstract()) {
      makeAbstract();
    } else {
      makeConcrete();
    }
    isClass = metadata.isClass();
    isAnnotation = metadata.isAnnotation();
    isEnum = metadata.isEnum();
    addInterfaces(metadata.getInterfaces());
    superClass = metadata.getSuperClass();
    definition = metadata.getClassName();

    getImports().addImports(metadata.getImports());

    if (metadata.hasGenerics()) {
      generics.clear();
      final String[] generic = metadata.getGenerics();
      for (final String s : generic) {
        generics.add(s);
      }
    }

    if (definition.contains(" ")) {
      throw new TypeDefinitionException("Found ambiguous class definition in "
          + definition);
    }
    if (definition.length() == 0) {
      throw new TypeDefinitionException(
          "Did not have a class name in class definition " + definition);
    }
    simpleName = definition;
    return this;
  }

  public ClassBuffer addInterfaces(final String... interfaces) {
    for (final String superInterface : interfaces) {
      addInterface(superInterface);
    }
    return this;
  }

  public ClassBuffer addInterface(final String iface) {
    return addInterface(iface, true);
  }

  public ClassBuffer addInterface(String iface, final boolean doImport) {
    iface = iface.trim();
    if (doImport) {
      final int ind = iface.indexOf('.');
      if (ind > 0 && Character.isLowerCase(iface.charAt(0))) {
        iface = context.getImports().addImport(iface);
      }
    }
    this.interfaces.add(iface);
    return this;
  }

  public ClassBuffer addInterfaces(final Class<?>... interfaces) {
    for (final Class<?> superInterface : interfaces) {
      addInterface(superInterface);
    }
    return this;
  }

  public ClassBuffer addInterface(final Class<?> iface) {
    assert iface.isInterface();
    final String name = context.getImports().addImport(iface.getCanonicalName());
    this.interfaces.add(name);
    return this;
  }

  @Override
  public ImportSection getImports() {
    return context.getImports();
  }

  @Override
  public String addImport(final String importName) {
    // Don't import types in the same package as us.
    if (getPackage() == null) {
      throw new NullPointerException(
          "ClassBuffer package not yet set; use .setPackage() on your SourceBuilder for "
              + this);
    }
    if (importName.startsWith(getPackage())) {
      if ("".equals(getPackage())) {
        if (importName.indexOf('.') == -1) {
          return importName;
        }
      }
      // Make sure it's not in a sub-package
      final String stripped = importName.substring(getPackage().length() + 1);
      // Assuming java camel case naming convention.
      if (Character.isUpperCase(stripped.charAt(0))) {
        // This is our package. Don't import, but do try to strip package.
        if (context.getImports().canMinimize(importName)) {
          context.getImports().tryReserveSimpleName(
              stripped.substring(stripped.lastIndexOf('.') + 1), importName);
          return stripped;
        } else {
          return importName;
        }
      }
    }
    return context.getImports().addImport(importName);
  }

  @Override
  public String addImport(final Class<?> cls) {
    return context.getImports().addImport(cls);
  }

  @Override
  public String addImportStatic(final Class<?> cls, final String name) {
    return context.getImports().addStaticImport(cls, name);
  }

  @Override
  public String addImportStatic(final String cls, final String name) {
    return context.getImports().addStaticImport(cls, name);
  }

  @Override
  public String addImportStatic(final String importName) {
    return context.getImports().addStaticImport(importName);
  }

  public String getSuperClass() {
    return superClass;
  }

  public ClassBuffer setSuperClass(final String superClass) {
    this.superClass = addImport(superClass);
    return this;
  }

  public ClassBuffer setSuperClass(final Class<?> superClass) {
    this.superClass = addImport(superClass);
    return this;
  }

  public ClassBuffer setPackage(String pkgName) {
    context.setPackage(pkgName);
    return this;
  }

  public String getPackage() {
    return context.getPackage();
  }

  public String getSimpleName() {
    return simpleName;
  }

  public String getQualifiedName() {
    final String pkg = context.getPackage();
    if (pkg.length() == 0) {
      return getSimpleName();
    }
    return pkg + "." + getSimpleName();
  }

  public void setSimpleName(final String className) {
    this.simpleName = className;
    for (final MethodBuffer buffer : ctors) {
      buffer.setName(className);
    }
  }

  public ClassBuffer createInnerClass(final String classDef) {
    final ClassBuffer inner = new ClassBuffer(context, this, memberIndent());
    inner.setDefinition(classDef, classDef.trim().endsWith("{"));
    addClass(inner);
    return inner;
  }

  public ClassBuffer createAnonymousClass(final String classDef) {
    ClassBuffer outer = this;
    class AnonymousClass extends ClassBuffer {
      public AnonymousClass(final SourceBuilder<?> context, final String indent) {
        super(context, outer, indent);
      }

      @Override
      public String toSource() {
        return NEW_LINE + origIndent + "new " + classDef + "() {" + NEW_LINE
            + superString();
      }

      @Override
      protected String memberIndent() {
        return indent + INDENT;
      }
    }
    final ClassBuffer inner = new AnonymousClass(context, indent + INDENT);
    inner.setDefinition(classDef, classDef.trim().endsWith("{"));
    addToEnd(inner);
    return inner;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void addToEnd(final CharBuffer buffer) {
    super.addToEnd(buffer);
    setNotIndent();
  }

  protected String memberIndent() {
    return origIndent + INDENT;
  }

  public MethodBuffer createConstructor(final int modifiers, final String... params) {
    final MethodBuffer method = new MethodBuffer(context, this, memberIndent());
    method.setModifier(modifiers);
    method.setName(getSimpleName());
    method.setReturnType("");
    method.addParameters(params);
    constructors.addToEnd(method);
    ctors.add(method);
    return method;
  }

  public MethodBuffer createMethod(final String methodDef) {
    final MethodBuffer method = new MethodBuffer(context, this, memberIndent());
    method.setDefinition(methodDef);
    addMethod(method);
    return method;
  }

  public MethodBuffer createMethod(final int modifiers, final Class<?> returnType,
      final String name, final String... params) {
    final MethodBuffer method = new MethodBuffer(context, this, memberIndent());
    method.setModifier(modifiers);
    method.setName(name);
    method.setReturnType(addImport(returnType));
    method.addParameters(params);
    addMethod(method);
    return method;
  }

  public FieldBuffer createField(final Class<?> type, final String name) {
    return createField(type.getCanonicalName(), name);
  }

  public FieldBuffer createField(final Class<?> type, final String name, final int modifier) {
    final FieldBuffer field = new FieldBuffer(this, type.getCanonicalName(), name,
        memberIndent());
    field.setModifier(modifier);
    addField(field);
    return field;
  }

  public FieldBuffer createField(final String type, final String name) {
    final FieldBuffer field = new FieldBuffer(this, type, name, memberIndent());
    addField(field);
    return field;
  }

  public FieldBuffer createField(final String type, final String name, final int modifier) {
    final FieldBuffer field = new FieldBuffer(this, type, name, memberIndent());
    field.setModifier(modifier);
    addField(field);
    return field;
  }

  private void addClass(final ClassBuffer clazz) {
    if (Modifier.isStatic(clazz.modifier)) {
      staticClasses().addToEnd(clazz);
    } else {
      classes.addToEnd(clazz);
    }
  }

  private void addField(final FieldBuffer field) {
    if (Modifier.isStatic(field.modifier)) {
      staticFields().addToEnd(field);
    } else {
      fields.addToEnd(field);
    }
  }

  private void addMethod(final MethodBuffer method) {
    if (Modifier.isStatic(method.modifier)) {
      staticMethods().addToEnd(method);
    } else {
      methods.addToEnd(method);
    }
  }

  private void ensureStatics() {
    if (staticFields == null) {
      staticFields = new PrintBuffer();
      staticClasses = new PrintBuffer();
      staticMethods = new PrintBuffer();
      statics.addToEnd(staticClasses);
      statics.addToEnd(staticFields);
      statics.addToEnd(staticMethods);
    }
  }

  private PrintBuffer staticFields() {
    ensureStatics();
    return staticFields;
  }

  private PrintBuffer staticMethods() {
    ensureStatics();
    return staticMethods;
  }

  private PrintBuffer staticClasses() {
    ensureStatics();
    return staticClasses;
  }

  @Override
  protected String footer() {
    return isWellFormatted ? "" : "\n" + origIndent + "}\n";
  }

  @Override
  public final ClassBuffer makeAbstract() {
    return super.makeAbstract();
  }

  protected ClassBuffer newChild() {
    return new ClassBuffer(this);
  }

  protected ClassBuffer newChild(final StringBuilder suffix) {
    return new ClassBuffer(this, suffix);
  }

  @Override
  @SuppressWarnings("unchecked")
  public ClassBuffer printAfter(String suffix) {
    return (ClassBuffer) super.printAfter(suffix);
  }

}

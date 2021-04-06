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
import xapi.fu.Printable;
import xapi.fu.java.X_Jdk;
import xapi.source.read.JavaLexer;
import xapi.source.read.JavaModel.IsParameter;
import xapi.source.read.JavaVisitor.AnnotationMemberVisitor;
import xapi.source.read.JavaVisitor.MethodVisitor;
import xapi.source.read.JavaVisitor.ParameterVisitor;
import xapi.source.read.JavaVisitor.TypeData;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

import static xapi.fu.itr.ArrayIterable.iterate;
import static xapi.source.read.JavaVisitor.MODIFIER_DEFAULT;

public class MethodBuffer extends MemberBuffer<MethodBuffer> implements
    MethodVisitor<SourceBuilder<?>>, CanAddImports {

  protected SourceBuilder<?>                         context;
  private boolean                                    once;
  private boolean                                    useJsni = true;
  private String                                     methodName;
  private final LinkedHashSet<String>                parameters;
  private final LinkedHashSet<String>                exceptions;
  private TypeData                                   returnType;
  private int                                        tryDepth;
  private final LinkedHashMap<String, LocalVariable> variables;

  public MethodBuffer(final SourceBuilder<?> context) {
    this(context, context.getClassBuffer(), INDENT);
  }

  private MethodBuffer(MethodBuffer from) {
    super(from.indent, from.enclosing);
    this.context = from.context;
    this.methodName = from.methodName;
    this.parameters = from.parameters;
    this.exceptions = from.exceptions;
    this.returnType = from.returnType;
    this.tryDepth = from.tryDepth;
    this.variables = from.variables;
  }

  private MethodBuffer(MethodBuffer from, StringBuilder target) {
    super(target, from.enclosing);
    this.indent = from.indent;
    this.context = from.context;
    this.methodName = from.methodName;
    this.parameters = from.parameters;
    this.exceptions = from.exceptions;
    this.returnType = from.returnType;
    this.tryDepth = from.tryDepth;
    this.variables = from.variables;
  }

  public MethodBuffer(final SourceBuilder<?> context, ClassBuffer enclosing, final String indent) {
    super(indent, enclosing);
    this.context = context;
    this.indent = indent + INDENT;
    parameters = new LinkedHashSet<>();
    exceptions = new LinkedHashSet<>();
    variables = new LinkedHashMap<>();
  }

  @Override
  public String toSource() {
    final StringBuilder b = new StringBuilder(Printable.NEW_LINE);
    if (javaDoc != null && javaDoc.isNotEmpty()) {
      b.append(javaDoc.toSource());
    }
    b.append(origIndent);
    if (annotations.size() > 0) {
      for (final String anno : annotations) {
        b.append('@').append(anno).append(Printable.NEW_LINE).append(origIndent);
      }
    }
    boolean isDefault = (modifier & MODIFIER_DEFAULT) == MODIFIER_DEFAULT;
    String mods = Modifier.toString(modifier);
    if (enclosing instanceof ClassBuffer && ((ClassBuffer)enclosing).isInterface()) {
      // remove public and abstract for interface methods, as they are implicit.
      mods = mods.replace("public", "").replace("abstract", "").replaceAll("\\s{2,}", " ");
    }
    while (mods.startsWith(" ")) {
      mods = mods.substring(1);
    }
    b.append(mods);
    if (isDefault) {
      b.append((mods.length() == 1 ? "" : " ")).append("default");
    }
    if (returnType.simpleName.length() > 0) {
      b.append(" ");
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
    // return type
    b.append(returnType);
    b.append(" ");
    if (methodName.length() > 0) {
      // method name
      b.append(methodName);
    }
    // parameters
    b.append(" (");
    String prefix = "";
    for (final String parameter : parameters) {
      b.append(prefix).append(parameter);
      prefix = ", ";
    }
    b.append(") ");
    if (!exceptions.isEmpty()) {
      b.append("\n" + indent + "  throws ");
      prefix = "";
      for (final String exception : exceptions) {
        b.append(prefix).append(exception);
        prefix = ", ";
      }
    }
    final String suffix;
    if (Modifier.isAbstract(modifier)) {
      prefix = ";\n";
      suffix = "";
    } else if (Modifier.isNative(modifier)) {
      if (useJsni) {
        prefix = "/*-{\n";
        suffix = (once ? Printable.NEW_LINE : "") + origIndent + "}-*/;\n";
      } else {
        prefix = ";\n";
        suffix = "";
      }
    } else {
      prefix = "{\n";
      suffix = (once ? Printable.NEW_LINE : "") + origIndent + "}\n";
    }
    return b.toString() + prefix + super.toSource() + suffix;
  }

  public String toStringBodyOnly() {
    return super.toSource();
  }

  public MethodBuffer addExceptions(final String... exceptions) {
    addTypes(this.exceptions, exceptions);
    return this;
  }

  @Override
  public ImportSection getImports() {
    return context.getImports();
  }

  @Override
  public String addImport(final Class<?> cls) {
    return context.getImports().addImport(cls);
  }

  @Override
  public String addImport(final String cls) {
    final String noPkg =
        context.getPackage() == null || context.getPackage().isEmpty() ? cls
            : cls.replace(context.getPackage() + ".", "");
    if (noPkg.indexOf('.') == -1) {
      return context.getImports().tryReserveSimpleName(noPkg, cls);
    }
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
  public String addImportStatic(final String cls) {
    return context.getImports().addStaticImport(cls);
  }

  public MethodBuffer addParameters(final String... parameters) {
    for (final String parameter : parameters) {
      final IsParameter param = JavaLexer.lexParam(parameter);
      final String toAdd = param.toString();
      this.parameters.add(toAdd);
    }
    return this;
  }

  public MethodBuffer addParameter(final Class<?> type, final String name) {
    final String typeName = addImport(type);
    final String toAdd = typeName + " " + name;
    this.parameters.add(toAdd);
    return this;
  }

  public MethodBuffer addParameter(final Class<?> type, final String name, String ... typeParams) {
    String typeName = addImport(type);
    if (typeParams.length > 0) {
      typeName += iterate(typeParams)
          .map(this::addImport)
          .join("<", ", ", ">");
    }
    final String toAdd = typeName + " " + name;
    this.parameters.add(toAdd);
    return this;
  }

  public MethodBuffer addParameter(final String type, final String name) {
    final String typeName = addImport(type);
    final String toAdd = typeName + " " + name;
    this.parameters.add(toAdd);
    return this;
  }

  @SuppressWarnings("unchecked")
  public MethodBuffer addParameters(final Entry<String, Class<?>>... parameters) {
    return addParameters(Arrays.asList(parameters));
  }

  public MethodBuffer addParameters(final Iterable<Entry<String, Class<?>>> parameters) {
    addNamedTypes(this.parameters, parameters);
    return this;
  }

  public MethodBuffer addExceptions(final Class<?>... exceptions) {
    addTypes(this.exceptions, exceptions);
    return this;
  }

  public MethodBuffer setExceptions(final Class<?>... exceptions) {
    this.exceptions.clear();
    addTypes(this.exceptions, exceptions);
    return this;
  }

  public MethodBuffer setExceptions(final String... exceptions) {
    this.exceptions.clear();
    addTypes(this.exceptions, exceptions);
    return this;
  }

  /**
   * Uses {@link JavaLexer} to extract a MethodBuffer definition.
   * <p>
   * This is slower than manually setting method metadata, but it does
   * automatically import fully qualified class names (if and only if there is
   * not already an imported type matching imported simple name).
   *
   * @param definition
   *          - Any valid java method definition. "public void doSomething()"
   * @return - A method buffer initialized to whatever the provided text lexes.
   *         <p>
   *         Report any parsing errors to github.com/WeTheInternet/com.google.gwt.thirdparty.xapi and/or
   *         james@wetheinter.net
   */
  public MethodBuffer setDefinition(final String definition) {
    // JavaMetadata will extract all modifiers for us
    JavaLexer.visitMethodSignature(this, context, definition, 0);
    return this;
  }

  public MethodBuffer setName(final String name) {
    methodName = name;
    return this;
  }

  public MethodBuffer setParameters(final String... parameters) {
    this.parameters.clear();
    return addParameters(parameters);
  }

  @SuppressWarnings("unchecked")
  public MethodBuffer setParameters(final Entry<String, Class<?>>... parameters) {
    this.parameters.clear();
    return addParameters(Arrays.asList(parameters));
  }

  public MethodBuffer setParameters(final Iterable<Entry<String, Class<?>>> parameters) {
    this.parameters.clear();
    return addParameters(parameters);
  }

  public MethodBuffer setReturnType(final Class<?> cls) {
    final String pkgName = cls.getPackage().getName();
    if (pkgName.length() == 0) {
      returnType = new TypeData("", cls.getCanonicalName());
    } else {
      returnType = new TypeData(pkgName, cls.getCanonicalName().replace(
        pkgName + ".", ""));
    }
    return this;
  }

  public MethodBuffer setReturnType(final String pkgName, final String enclosedClassName) {
    returnType = new TypeData(pkgName, enclosedClassName);
    return this;
  }

  public MethodBuffer setReturnType(final String canonicalName) {
    if ("".equals(canonicalName)) {
      returnType = new TypeData("");
    } else {
      returnType = JavaLexer.extractType(canonicalName, 0);
    }
    return this;
  }

  public TypeData getReturnType() {
    return returnType;
  }

  public ClassBuffer createLocalClass(final String classDef) {
    final ClassBuffer cls = new ClassBuffer(context, this, indent);
    cls.setDefinition(classDef, false);
    assert cls.privacy == 0 : "A local class cannot be "
      + Modifier.toString(cls.privacy);
    addToEnd(cls);
    setNotIndent();
    return cls;
  }

  @Override
  public void onAppend() {
    if (once) {
      once = false;
      onFirstAppend();
    }
    super.onAppend();
  }

  protected void onFirstAppend() {
    if (enclosing instanceof ClassBuffer && ((ClassBuffer)enclosing).isInterface()) {
      // automatically add default if you are printing into a non-static interface method
      if (!Modifier.isStatic(modifier)) {
        modifier |= MODIFIER_DEFAULT;
      }
    }
  }

  /**
   * @param useJsni
   *          - Whether to encapsulate native methods with /*-{ }-* /
   * @return
   */
  public MethodBuffer setUseJsni(final boolean useJsni) {
    this.useJsni = useJsni;
    modifier = modifier | Modifier.NATIVE;
    return this;
  }

  public final MethodBuffer makeJsni() {
    setUseJsni(true).makeNative();
    return this;
  }

  public final MethodBuffer makeNative() {
    if ((modifier & Modifier.ABSTRACT) > 0)
    {
      modifier &= ~Modifier.ABSTRACT;// "Cannot be both native and abstract";
    }
    modifier = modifier | Modifier.NATIVE;
    return this;
  }

  @Override
  public final MethodBuffer makeAbstract() {
    return super.makeAbstract();
  }

  /**
   * Add a return clause; the return keyword and semicolon are optional.
   * <p>
   * If you send "throw someException()", a return will not be added.
   * <p>
   * This allows you to use the returnValue() to optionally throw instead of
   * return.
   *
   * @param expr
   * @return
   */
  public MethodBuffer returnValue(final String expr) {
    return println(validateForReturn(expr));
  }

  private String validateForReturn(String expr) {
    return (expr.matches("\\s*(throw|return)\\s.*") ? "" : "return ")
        + expr + (expr.endsWith(";") ? "" : ";");
  }

  public MethodBuffer returnPattern(final String expr, Object replace1, Object ... replacements) {
    return patternln(validateForReturn(expr), replace1, replacements);
  }
  public PrintBuffer stmtReturn() {
    PrintBuffer ret = new PrintBuffer(getIndentCount()) {
      @Override
      public String toSource() {
        String source = super.toSource();
        source = source.endsWith(";") ? source.substring(0, source.length()-1) : source;
        assert !source.trim().startsWith("return ") : "Do not add `return ` to the start of a stmtReturn";
        return source;
      }

      @Override
      public PrintBuffer clear() {
        indented = true;
        return super.clear();
      }
    };
    print("return ");
    ret.indented = true;
    addToEnd(ret);
    println(";");
    return ret;
  }

  @Override
  public ParameterVisitor<SourceBuilder<?>> visitParameter() {
    return new ParameterVisitor<SourceBuilder<?>>() {

      int                         modifier;
      private final SimpleStack<String> annotations = new SimpleStack<String>();

      @Override
      public AnnotationMemberVisitor<SourceBuilder<?>> visitAnnotation(
          String annoName, final String annoBody, final SourceBuilder<?> receiver) {
        annoName = addImport(annoName.startsWith("@") ? annoName.substring(1)
          : annoName);
        annotations.add("@" + annoName
          + (annoBody.length() > 0 ? "(" + annoBody + ")" : ""));
        return null;
      }

      @Override
      public void visitModifier(final int modifier, final SourceBuilder<?> receiver) {
        this.modifier |= modifier;
      }

      @Override
      public void visitType(final TypeData type, final String name, final boolean varargs,
          final SourceBuilder<?> receiver) {
        if (type.pkgName.length() > 0) {
          receiver.getImports().addImport(type.getImportName());
        }
        final StringBuilder b = new StringBuilder();
        for (final String anno : annotations) {
          b.append(anno).append(' ');
        }
        final String mod = Modifier.toString(modifier);
        if (mod.length() > 0) {
          b.append(mod).append(" ");
        }

        if (varargs) {
          b.append(type.getSimpleName().replace("[]", "") + " ... " + name);
        } else {
          b.append(type.getSimpleName() + " " + name);
        }
        parameters.add(b.toString());
      }
    };
  }

  @Override
  public void visitException(final String type, final SourceBuilder<?> receiver) {
    exceptions.add(type);
  }

  @Override
  public AnnotationMemberVisitor<SourceBuilder<?>> visitAnnotation(
      final String annoName, final String annoBody, final SourceBuilder<?> receiver) {
    addAnnotation("@" + annoName
      + (annoBody.trim().length() > 0 ? "(" + annoBody + ")" : ""));
    return null;
  }

  @Override
  public void visitModifier(final int modifier, final SourceBuilder<?> receiver) {
    assert validModification(this.modifier, modifier);
    setModifier(modifier);
  }

  private boolean validModification(final int modifier, final int change) {
    if ((change & Modifier.ABSTRACT) > 0) {
      if ((modifier & Modifier.STATIC) > 0) {
        throw new AssertionError("You cannot make a static method abstract.\n"
          + this);
      }
      if ((modifier & Modifier.FINAL) > 0) {
        throw new AssertionError("You cannot make a final method abstract.\n"
          + this);
      }
    }
    if ((modifier & Modifier.ABSTRACT) > 0) {
      if ((change & Modifier.STATIC) > 0) {
        throw new AssertionError("You cannot make an abstract method static.\n"
          + this);
      }
      if ((change & Modifier.FINAL) > 0) {
        throw new AssertionError("You cannot make an abstract method final.\n"
          + this);
      }
    }
    return true;
  }

  @Override
  public void visitGeneric(String generic, final SourceBuilder<?> receiver) {
    generic = generic.trim();
    if (generic.charAt(0) == '<') {
      generic = generic.substring(1, generic.length() - 1);
    }
    for (final String importable : JavaLexer.findImportsInGeneric(generic)) {
      final String imported = receiver.getImports().addImport(importable);
      if (importable.length() != imported.length()) {
        int len = -1;
        while (len != generic.length()) {
          len = generic.length();
          generic = generic.replace(importable, imported);
        }
      }
    }
    generics.add(generic);
  }

  @Override
  public void visitJavadoc(final String javadoc, final SourceBuilder<?> receiver) {

  }

  @Override
  public void visitReturnType(final TypeData returnType, final SourceBuilder<?> receiver) {
    this.returnType = returnType;
    if (returnType.pkgName.length() > 0) {
      receiver.getImports().addImport(returnType.getImportName());
    }
  }

  @Override
  public void visitName(final String name, final SourceBuilder<?> receiver) {
    methodName = name;
  }

  public MethodBuffer startTry() {
    tryDepth++;
    println("try {");
    indent();
    return this;
  }

  public MethodBuffer startTry(final String withResources) {
    tryDepth++;
    println("try (" + withResources + ") {");
    indent();
    return this;
  }

  public MethodBuffer startCatch(final String exceptionType, final String exceptionName) {
    outdent();
    println("} catch (" + exceptionType + " " + exceptionName + ") {");
    indent();
    return this;
  }

  public String startCatch(String exceptionType) {
    outdent();
    final int ind = exceptionType.lastIndexOf(' ');
    String name;
    if (ind == -1) {
      name = "e" + tryDepth;
      exceptionType = exceptionType + " " + name;
    } else {
      name = exceptionType.substring(ind + 1);
    }
    println("} catch (" + exceptionType + ") {");
    indent();
    return name;
  }

  public MethodBuffer startFinally() {
    outdent();
    println("} finally {");
    indent();
    return this;
  }

  public MethodBuffer endTry() {
    outdent();
    println("}");
    tryDepth--;
    return this;
  }

  protected MethodBuffer newChild() {
    return new MethodBuffer(this);
  }

  protected MethodBuffer newChild(final StringBuilder suffix) {
    return new MethodBuffer(this, suffix);
  }

  @Override
  @SuppressWarnings("unchecked")
  public MethodBuffer printAfter(String suffix) {
    return (MethodBuffer) super.printAfter(suffix);
  }

  public String getName() {
    return methodName;
  }

  public MethodBuffer throwException(Class<? extends Throwable> type) {
    String imported = addImport(type);
    println("throw new "+imported+"();");
    return this;
  }

  public MethodBuffer throwException(Class<? extends Throwable> type, String constructorArgs) {
    String imported = addImport(type);
    println("throw new "+imported+"(" + constructorArgs+");");
    return this;
  }

  public LocalVariable newVariable(Class<?> type, String name) {
    return newVariable(addImport(type), name);
  }

  public LocalVariable newVariable(String type, String name) {
    return newVariable(type, name, false);
  }

  public LocalVariable newVariable(Class<?> type, String name, boolean reuseExisting) {
    return newVariable(addImport(type), name, reuseExisting);
  }

  public String reserveVariable(String name) {
    if (!variables.containsKey(name)) {
      variables.put(name, null);
      return name;
    }
    int unused = 0;
    while(variables.containsKey(name + "_" + unused)) {
      unused++;
    }
    name = name + "_" + unused;
    variables.put(name, null);
    return name;
  }

  public LocalVariable newVariable(String type, String name, boolean reuseExisting) {
    final LocalVariable var;
    final LocalVariable was = variables.get(name);
    if (!variables.containsKey(name)) {
      // easy-peasy
      var = new LocalVariable(this, type, name, getIndent());
    } else if (was != null && reuseExisting){
      // caller wants a stable name here...
      // TODO: record something to elide the type information for the var reuse...
      // so, return a modified duplicate here w/ doesn't print the type  again
      // (to get this right, we would actually need a call stack matching printed code stack...
      // which is unlikely to ever happen, as it is not a high value reward)
      return was;
    } else {
      name = reserveVariable(name);
      var = new LocalVariable(this, type, name, getIndent());
    }
    variables.put(name, var);
    boolean finishLine = !isIndentNeeded();
    if (finishLine) {
      println();
    }
    addToEnd(var);
    println();
    return var;
  }

  public boolean hasVariable(String name) {
    return variables.containsKey(name);
  }

  public Map<String, LocalVariable> getVariables() {
    return Collections.unmodifiableMap(variables);
  }

  public ClassBuffer getEnclosingClass() {
    MemberBuffer<?> enclosed = getEnclosing();
    while (enclosed != null) {
      if (enclosed instanceof ClassBuffer) {
        return (ClassBuffer) enclosed;
      }
      assert enclosed != enclosed.getEnclosing() : enclosed + " returns itself from getEnclosing()!";
      enclosed = enclosed.getEnclosing();
    }
    return null;
  }

  public SourceBuilder<?> getContext() {
    return context;
  }
}

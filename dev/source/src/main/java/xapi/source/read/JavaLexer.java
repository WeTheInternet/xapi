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

package xapi.source.read;

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import xapi.collect.impl.SimpleStack;
import xapi.dev.source.TypeDefinitionException;
import xapi.source.read.JavaModel.AnnotationMember;
import xapi.source.read.JavaModel.HasAnnotations;
import xapi.source.read.JavaModel.HasModifier;
import xapi.source.read.JavaModel.IsAnnotation;
import xapi.source.read.JavaModel.IsGeneric;
import xapi.source.read.JavaModel.IsParameter;
import xapi.source.read.JavaModel.IsType;
import xapi.source.read.JavaVisitor.AnnotationMemberVisitor;
import xapi.source.read.JavaVisitor.AnnotationVisitor;
import xapi.source.read.JavaVisitor.ClassVisitor;
import xapi.source.read.JavaVisitor.GenericVisitor;
import xapi.source.read.JavaVisitor.JavadocVisitor;
import xapi.source.read.JavaVisitor.MethodVisitor;
import xapi.source.read.JavaVisitor.ModifierVisitor;
import xapi.source.read.JavaVisitor.ParameterVisitor;
import xapi.source.read.JavaVisitor.TypeData;

public class JavaLexer {

  public static class ModifierExtractor implements ModifierVisitor<HasModifier> {

    @Override
    public void visitModifier(int modifier, HasModifier receiver) {
      receiver.modifier |= modifier;
    }

  }
  public static class AnnotationExtractor implements AnnotationVisitor<HasAnnotations> {

    @Override
    public AnnotationMemberVisitor<HasAnnotations> visitAnnotation(String annoName, String annoBody, HasAnnotations receiver) {
      IsAnnotation anno = new IsAnnotation(annoName);
      if (receiver != null)
        receiver.addAnnotation(anno);
      return new AnnotationMemberExtractor();
    }
  }

  public static class JavadocExtractor implements JavadocVisitor<StringBuilder> {
    @Override
    public void visitJavadoc(String javadoc, StringBuilder receiver) {
      // TODO: remove *s
      receiver.append(javadoc);
    }
  }

  public static class AnnotationMemberExtractor implements AnnotationMemberVisitor<HasAnnotations> {

    @Override
    public void visitMember(String name, String value, HasAnnotations receiver) {
      assert !receiver.annotations.isEmpty() :
        "You must visit an annotation before visiting an annotation member";
      receiver.annotations.tail().members.add(new AnnotationMember(name, value));
    }

  }

  public static class GenericsExtractor implements GenericVisitor<SimpleStack<IsGeneric>> {
    @Override
    public void visitGeneric(String generic, SimpleStack<IsGeneric> receiver) {
      if (generic.charAt(0) == '<') {
        generic = generic.substring(1, generic.length()-1);
      }
      receiver.add(new IsGeneric(generic, ""));
    }
  }

  /**
   * We need to store an index with our type data,
   * so our lexer method can return a new type object,
   * along with the number of character read while lexing.
   *
   * @author "James X. Nelson (james@wetheinter.net)"
   *
   */
  public static class TypeDef extends TypeData {

    int index;
    public boolean varargs;

    public TypeDef(String name) {
      super(name);
    }

    public TypeDef(String name, int index) {
      super(name);
      this.index = index;
    }

    public boolean isArray() {
      return arrayDepth > 0;
    }
  }

  protected static class MemberData {

    protected final int modifier;
    protected final String simpleName;
    protected final String typeName;
    protected final String javaDoc;
    protected final Set<String> generics;
    protected final Set<String> imports;
    protected final Set<String> annotations;

    protected MemberData(int modifier, String simpleName, String typeName,
        String javaDoc) {
      this.modifier = modifier;
      this.simpleName = simpleName;
      this.typeName = typeName;
      this.javaDoc = javaDoc;
      generics = new TreeSet<String>();
      imports = new TreeSet<String>();
      annotations = new TreeSet<String>();
    }
  }

  public static <R> int visitJavadoc
  (JavadocVisitor<R> visitor, R receiver, CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (pos == chars.length())
      return pos;
    try {
      if ('/' == chars.charAt(pos)) {
        if (chars.charAt(++pos) == '*') {
          int start = pos + (chars.charAt(pos) == '*' ? 1 : 0);
          // We hava some javadoc. Let's eat it all
          do {
            while (chars.charAt(++pos) != '*')
              ;
          } while (chars.charAt(++pos) != '/');
          chars.subSequence(start, pos - 2);
          visitor.visitJavadoc(chars.toString().replaceAll("\n\\s*[*]", "")
              // eat opening \n * javadoc chars
              , receiver);
        }
      }
    } catch (IndexOutOfBoundsException e) {
      error(e, "Error parsing javadoc on: " + chars.toString());
    }
    return pos;
  }

  public static <R> int visitAnnotation
  (AnnotationVisitor<R> visitor, R receiver, CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (pos == chars.length())
      return pos;
    int start = pos;
    try {
      while(chars.charAt(pos) == '@') {
        pos = eatJavaname(chars, pos + 1);
        String annoName = chars.subSequence(start + 1, pos).toString();
        String annoBody = "";
        pos = eatWhitespace(chars, pos);
        if (pos < chars.length() && chars.charAt(pos) == '(') {
          // Annotation has a body
          int bodyStart = pos+1;
          pos = eatAnnotationBody(visitor, receiver, chars, pos);
          annoBody = chars.subSequence(bodyStart, pos).toString();
          pos ++;
        }
        AnnotationMemberVisitor<R> bodyVisitor = visitor.visitAnnotation(annoName, annoBody, receiver);
        if (bodyVisitor != null && annoBody.length() > 0) {
          visitAnnotationMembers(bodyVisitor, receiver, annoBody, 0);
        }
        start = pos = eatWhitespace(chars, pos);
        if (pos == chars.length())
          break;
      }
    } catch (IndexOutOfBoundsException e) {
      error(
          e,
          "Error parsing annotation on: "
              + chars.subSequence(start, chars.length()));
    }
    return pos;
  }

  public static <R> int visitAnnotationMembers
  (AnnotationMemberVisitor<R> visitor, R receiver, CharSequence chars, int pos) {
    String name = "value";
    boolean nameNext = true;
    while (true) {
      if (pos == chars.length())
        return pos;
      pos = eatWhitespace(chars, pos);
      switch (chars.charAt(pos)) {
      case ',':
        nameNext = true;
      case ' ':
      case '\n':
      case '\t':
      case '\r':
        continue;
      case ')':
        return pos;
      // In case there is a value field without the name, we need to skip extracting the name.
      case '{':
      case '"':
      case '@':
        name = "value";
        nameNext = false;
      default:
        if (nameNext) {
          nameNext = false;
          int start = pos;
          while (Character.isJavaIdentifierPart(chars.charAt(pos)))pos++;
          String maybeName = chars.subSequence(start, pos).toString().trim();
          if (maybeName.length() == 0)
            name = "value";
          else
            name = maybeName;
          pos = eatWhitespace(chars, pos);
          switch (chars.charAt(pos)) {
          case '=': // assignment
            nameNext = false;
            continue;
          case ',': // end of a value= without explicit "value="
            visitor.visitMember("value", maybeName, receiver);
            nameNext = true;
            continue;
          case ')': // end of a value= without more items
            visitor.visitMember("value", maybeName, receiver);
            nameNext = true;
            return pos;
          default:
            if (pos == chars.length()) {
              visitor.visitMember("value", maybeName, receiver);
              return pos;
            }
            pos--;
            continue;
          }
        } else {
          // there's a variable to read
          int start = pos;
          switch (chars.charAt(pos)) {
          case '{':
            pos = eatArrayInitializer(chars, pos);

            break;
          case '"':
            pos = eatStringValue(chars, pos);
            if (chars.charAt(pos) == '"')
              pos++;
            break;
          case '@':
            AnnotationVisitor<HasAnnotations> extractor = new AnnotationExtractor();
            pos = visitAnnotation(extractor, null, chars, pos);
            break;
          default:
            char c = chars.charAt(pos);
            while (!Character.isWhitespace(c)) {
              c = chars.charAt(++pos);
            }
          }
          visitor.visitMember(name, chars.subSequence(start, pos).toString(), receiver);
        }
      }
    }
  }

  public static <R> int visitModifier
  (ModifierVisitor<R> visitor, R receiver, CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    while(true) {

      char c = chars.charAt(pos);
      switch (c){
      case 'p':
        // public, protected, private
        if (chars.subSequence(pos, pos+7).equals("public ")) {
          pos = eatWhitespace(chars, pos+7);
          visitor.visitModifier(Modifier.PUBLIC, receiver);
          continue;
        }
        else if (chars.subSequence(pos, pos+8).equals("private ")) {
          pos = eatWhitespace(chars, pos+8);
          visitor.visitModifier(Modifier.PRIVATE, receiver);
          continue;
        }
        else if (chars.subSequence(pos, pos+10).equals("protected ")) {
          pos = eatWhitespace(chars, pos+10);
          visitor.visitModifier(Modifier.PROTECTED, receiver);
          continue;
        }
        return pos;
      case 'f':
        // final
        if (chars.subSequence(pos, pos+6).equals("final ")) {
          pos = eatWhitespace(chars, pos+6);
          visitor.visitModifier(Modifier.FINAL, receiver);
          continue;
        }
        return pos;
      case 'a':
        // abstract
        if (chars.subSequence(pos, pos+9).equals("abstract ")) {
          pos = eatWhitespace(chars, pos+9);
          visitor.visitModifier(Modifier.ABSTRACT, receiver);
          continue;
        }
        return pos;
      case 's':
        // static, synchronized, strictfp
        if (chars.subSequence(pos, pos+7).equals("static ")) {
          pos = eatWhitespace(chars, pos+7);
          visitor.visitModifier(Modifier.STATIC, receiver);
          continue;
        }
        else if (chars.subSequence(pos, pos+13).equals("synchronized ")) {
          pos = eatWhitespace(chars, pos+13);
          visitor.visitModifier(Modifier.SYNCHRONIZED, receiver);
          continue;
        }
        else if (chars.subSequence(pos, pos+9).equals("strictfp ")) {
          pos = eatWhitespace(chars, pos+9);
          visitor.visitModifier(Modifier.STRICT, receiver);
          continue;
        }
        return pos;
      case 'n':
        // native
        if (chars.subSequence(pos, pos+7).equals("native ")) {
          pos = eatWhitespace(chars, pos+7);
          visitor.visitModifier(Modifier.NATIVE, receiver);
          continue;
        }
        return pos;
      case 't':
        // transient
        if (chars.subSequence(pos, pos+10).equals("transient ")) {
          pos = eatWhitespace(chars, pos+10);
          visitor.visitModifier(Modifier.TRANSIENT, receiver);
          continue;
        }
        return pos;
      case 'v':
        // volatile
        if (chars.subSequence(pos, pos+9).equals("volatile ")) {
          pos = eatWhitespace(chars, pos+9);
          visitor.visitModifier(Modifier.VOLATILE, receiver);
          continue;
        }
        return pos;
      default :
        return pos;
      }
    }
  }

  public static <R> int visitGeneric
  (GenericVisitor<R> visitor, R receiver,
      CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (chars.charAt(pos) == '<') {
      int start = pos;
      pos = eatGeneric(chars, pos)+1;
      visitor.visitGeneric(chars.subSequence(start, pos).toString(), receiver);
    }
    return eatWhitespace(chars, pos);
  }

  public static <R> int visitMethodSignature
  (MethodVisitor<R> visitor, R receiver, CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (pos == chars.length())
      return pos;

    pos = visitAnnotation(visitor, receiver, chars, pos);
    pos = visitModifier(visitor, receiver, chars, pos);
    pos = visitGeneric(visitor, receiver, chars, pos);
    TypeDef returnType = extractType(chars, pos);
    visitor.visitReturnType(returnType, receiver);
    int start = pos = eatWhitespace(chars, returnType.index);
    if (chars.charAt(pos) == '.' && chars.charAt(pos+1) == '.') {
      pos = eatWhitespace(chars, pos+3);
      returnType.arrayDepth++;
    }
    pos = eatJavaname(chars, pos);
    visitor.visitName(chars.subSequence(start, pos).toString(), receiver);
    pos = eatWhitespace(chars, pos);
    if (pos == chars.length()) {
      return pos;
    }
    if (chars.charAt(pos) == '(') {
      // params
      pos = eatWhitespace(chars, pos + 1);
      while (chars.charAt(pos) != ')') {
        // TODO grab parameter annotations here.
        ParameterVisitor<R> param = visitor.visitParameter();
        pos = visitAnnotation(param, receiver, chars, pos);
        pos = visitModifier(param, receiver, chars, pos);

        TypeDef def = extractType(chars, pos);
        start = pos = eatWhitespace(chars, def.index);
        boolean varargs = false;
        if (chars.charAt(pos) == '.') {
          assert chars.charAt(pos+1)=='.';
          assert chars.charAt(pos+2)=='.';
          def.arrayDepth ++;
          start = pos = eatWhitespace(chars, pos+3);
          pos = eatJavaname(chars, start);
          varargs = true;
        } else {
          pos = eatJavaname(chars, start);
        }
        param.visitType(def, chars.subSequence(start, pos).toString(), varargs, receiver);
        pos = eatWhitespace(chars, pos);
        if (chars.charAt(pos) == ',')
          pos++;
      }
    }
    if (pos == chars.length())
      return pos;
    pos = eatWhitespace(chars, pos+1);
    if (pos == chars.length())
      return pos;
    // exceptions
    if (chars.charAt(pos) == 't') {
      if (chars.subSequence(pos, pos+6).equals("throws")) {
        pos = eatWhitespace(chars, pos+7);
        while (pos < chars.length()) {
          if (chars.charAt(pos)=='{' || chars.charAt(pos) == ';')
            return pos;
          start = pos;
          pos = eatJavaname(chars, pos);
          visitor.visitException(chars.subSequence(start, pos).toString(), receiver);
          pos = eatWhitespace(chars, pos);
          if (pos == chars.length())
            return pos;
          if (chars.charAt(pos) == ',')
            pos = eatWhitespace(chars, pos+1);
        }
      }
    }
    // defaults?
    return eatWhitespace(chars, pos);
  }

  /**
   * Extracts information from a fully qualified source name,
   * including the (unparsed) generics string, and array depth.
   *
   * Specifically, the signature must be
   * a "properly formatted" java naming-convention type definition.
   *
   * That is, package.names.must.be.lowercase.Class.Names.TitleCase
   *
   * This is intended to read in java source format only;
   * $ is treated as a regular, legal java identifier.
   *
   * This method is suitable to read field, parameter and variable declarations.
   *
   * @param chars - The type signature to read.
   * @param pos - Where to start reading
   * @return - The end index where we finished reading.
   *
   * Do not send strings which may include fully qualified method or field references;
   * instead use {@link #extractStatement(CharSequence, int)}
   *
   */
  public static TypeDef extractType(CharSequence chars, int pos)
  {
    int start = pos = eatWhitespace(chars, pos);
    int lastPeriod = -1, max = chars.length()-1;
    boolean doneParsing = false;
    StringBuilder pkg = new StringBuilder();
    package_loop:
    if (Character.isLowerCase(chars.charAt(pos))) {
      // We may have package names to read.
      while(true) {
        pos = eatWhitespace(chars, pos);
        while(Character.isJavaIdentifierPart(chars.charAt(++pos))){
          if (pos == max){
            if (lastPeriod == -1) {
              // no package, so don't go looking for class references either.
              doneParsing = true;
            } else {
              // everything up to the last period is already in package variable.
              start = pos = lastPeriod + 1;
            }
            break package_loop;
          }
        }
        int whitespace = eatWhitespace(chars, pos);
        int next = chars.charAt(whitespace);
        if (next == '.') {
          if (whitespace > pos && chars.charAt(whitespace+1)=='.') {
            break package_loop;
          }
          if (lastPeriod != -1)
            pkg.append('.');
          lastPeriod = pos = whitespace;
          pkg.append(chars.subSequence(start, pos).toString().trim());
          pos = start = eatWhitespace(chars, pos+1);
          if (Character.isUpperCase(chars.charAt(start))) {
            break package_loop;
          }
        } else {
          if (whitespace > pos || next == '[' || next == '<') {
            doneParsing = true;
            break package_loop;
          }
        }
      }
  }
  TypeDef def;
    if (doneParsing){
      if (pos == max)
        return new TypeDef(chars.subSequence(start, pos+1).toString(), pos);
      def = new TypeDef(chars.subSequence(start, pos).toString());
    } else {
      StringBuilder typeName = new StringBuilder();
      lastPeriod = -1;
      typeloop:
      while (true) {
        pos = eatWhitespace(chars, pos);
        if (!Character.isJavaIdentifierStart(chars.charAt(pos))){
          if (pos > start) {
            if (lastPeriod != -1)
              typeName.append('.');
            typeName.append(chars.subSequence(start, pos).toString().trim());
          }
          break;
        }
        while(Character.isJavaIdentifierPart(chars.charAt(++pos)))
          if (pos == max) {
            if (lastPeriod != -1)
              typeName.append('.');
            typeName.append(chars.subSequence(start, pos+1).toString().trim());
            break typeloop;
          }
        int whitespace = eatWhitespace(chars, pos);
        if (chars.charAt(whitespace) == '.') {
          if (lastPeriod != -1)
            typeName.append('.');
          if (pos != whitespace && chars.charAt(whitespace + 1) == '.') {
            typeName.append(chars.subSequence(start, pos).toString().trim());
            break;
          }
          lastPeriod = pos = whitespace;
          typeName.append(chars.subSequence(start, pos).toString());
          start = pos = eatWhitespace(chars, pos+1);
        } else {
          if (whitespace > pos) {
            if (lastPeriod != -1)
              typeName.append('.');
            typeName.append(chars.subSequence(start, pos).toString().trim());
            break;
          }
        }
      }
      def = new TypeDef(typeName.toString());
      if (pos == max)pos++;
    }
    def.pkgName = pkg.toString();
    start = pos = eatWhitespace(chars, pos);
    if (pos < chars.length()) {
      pos = eatGeneric(chars, pos);
      if (pos != start)
        def.generics = chars.subSequence(start, ++pos).toString();
    }
    pos = eatWhitespace(chars, pos);
    while (pos < max && chars.charAt(pos) == '[') {
      def.arrayDepth ++;
      while (chars.charAt(++pos)!=']');
      pos = eatWhitespace(chars, pos+1);
    }
    if (pos < chars.length() && chars.charAt(pos) == '.') {
      assert chars.charAt(pos+1) == '.';
      assert chars.charAt(pos+2) == '.';
      def.arrayDepth++;
      def.varargs = true;
      pos = eatWhitespace(chars, pos+3);
    }
    def.index = pos;
    return def;
  }

  public static SimpleStack<IsGeneric> extractGenerics (CharSequence chars, int pos) {
    SimpleStack<IsGeneric> stack = new SimpleStack<IsGeneric>();
    visitGeneric(new GenericsExtractor(), stack, chars, pos);
    return stack;
  }

  protected static <R> int eatAnnotationBody
  (AnnotationVisitor<R> visitor,R receiver, CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    boolean nameNext = true;
    while (true) {
      pos = eatWhitespace(chars, ++pos);
      switch (chars.charAt(pos)) {
      case ',':
        nameNext = true;
      case ' ':
      case '\n':
      case '\t':
      case '\r':
        continue;
      case ')':
        return pos;
      case '{':
        nameNext = false;
      default:
        if (nameNext) {
          nameNext = false;
          while (Character.isJavaIdentifierPart(chars.charAt(pos)))pos++;
          pos = eatWhitespace(chars, pos);
          switch (chars.charAt(pos)) {
          case '=': // assignment
            nameNext = false;
            continue;
          case ',': // end of a value= without explicit "value="
            // TODO set the name to "value"
            nameNext = true;
            continue;
          case ')': // end of a value= without more items
            return pos;
          default:
            pos--;
            continue;
          }
        } else {
          // there's a variable to read
          switch (chars.charAt(pos)) {
          case '{':
            pos = eatArrayInitializer(chars, pos);
            break;
          case '"':
            pos = eatStringValue(chars, pos);
            break;
          case '@':
            pos = visitAnnotation(visitor, receiver, chars, pos);
            break;
          default:
            char c = chars.charAt(pos);
            while (!Character.isWhitespace(c)) {
              c = chars.charAt(++pos);
            }
          }
        }
      }
    }
  }

  protected static <R > int eatWhitespace(CharSequence chars, int pos) {
    try {
      while (Character. isWhitespace(chars .charAt(pos)))
        pos++;
    } catch (IndexOutOfBoundsException ignored) {
    }
    return pos;
  }

  protected static int eatJavaname(CharSequence chars, int pos) {
    try {
      while (Character.isJavaIdentifierPart(chars.charAt(pos)))
        pos++;
      if (chars.charAt(pos) == '.')
        return eatJavaname(chars, pos + 1);
    } catch (IndexOutOfBoundsException ignored) {}
    return pos;
  }

  protected static <R> int eatGeneric(CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (pos == chars.length())return pos;
    if (chars.charAt(pos) == '<') {
      int genericDepth = 1;
      while (genericDepth > 0) {
        switch(chars.charAt(++pos)) {
        case '>':
          genericDepth--;
          break;
        case '<':
          genericDepth++;
          break;
        }
      }
    }
    return eatWhitespace(chars, pos);
  }

  protected static int eatStringValue(CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    try {
      switch (chars.charAt(pos)) {
      case 'n':
        assert chars.charAt(pos + 1) == 'u';
        assert chars.charAt(pos + 2) == 'l';
        assert chars.charAt(pos + 3) == 'l';
        return pos + 4;
      case '"':
        boolean escaped = false;
        char c;
        while ((c = chars.charAt(++pos)) != '"' || escaped)
          escaped = c == '\\' && !escaped;
      }
    } catch (IndexOutOfBoundsException e) {

    }
    return pos;
  }

  protected static int eatArrayValue(CharSequence chars, int pos) {
    int arrayDepth = 1;
    while (arrayDepth > 0) {
      char c = chars.charAt(++pos);
      switch (c) {
      case '"':
        pos = eatStringValue(chars, pos);
        break;
      case '[':
        arrayDepth++;
        break;
      case ']':
        arrayDepth--;
        break;
      }
    }
    return pos + 1;
  }

  protected static int eatArrayInitializer(CharSequence chars, int pos) {
    while (true) {
      char c = chars.charAt(++pos);
      if (c == '"')
        pos = eatStringValue(chars, pos);
      else if (c == '}')
        return pos;
    }
  }

  protected static boolean isQualified(String typeName) {
    // In order to avoid trying to import enclosed type names, like
    // Cls.InnerCls,
    // we require that recognized package names begin w/ a lowercase letter.
    //
    // So long as you stick to java naming conventions, the lexer will handle
    // imports
    // and qualified name shortening automatically.
    return Character.isLowerCase(typeName.charAt(0))
        && typeName.indexOf('.') != -1;
  }

  protected static void error(Throwable e, String string) {
    if (string != null)
      System.err.println(string);
    if (e != null)
      e.printStackTrace();
  }

  private final int modifier;

  private final boolean isClass;
  private final boolean isGenerics;
  private final Set<String> interfaces;
  private final Set<String> generics;
  private final Set<String> imports;
  private final String superClass;
  private final String className;

  public JavaLexer(String definition) {
    definition = definition.replace('\t', ' ');
    interfaces = new TreeSet<String>();
    generics = new TreeSet<String>();
    imports = new TreeSet<String>();
    String original = definition;
    int modifier = 0;
    if (definition.contains("public ")) {
      definition = definition.replace("public ", "");
      modifier = Modifier.PUBLIC;
    } else if (definition.contains("protected ")) {
      definition = definition.replace("protected ", "");
      modifier = Modifier.PROTECTED;
    } else if (definition.contains("private ")) {
      definition = definition.replace("private ", "");
      modifier = Modifier.PRIVATE;
    } else {
      modifier = 0;
    }
    // eat opening brackets; we will supply our own
    definition = definition.replace("{", "");

    if (definition.contains("static ")) {
      modifier |= Modifier.STATIC;
      definition = definition.replace("static ", "");
    }

    if (definition.contains("final ")) {
      modifier |= Modifier.FINAL;
      definition = definition.replace("final ", "");
    }

    if (definition.contains("native ")) {
      modifier |= Modifier.NATIVE;
      definition = definition.replace("native ", "");
    }

    if (definition.contains("synchronized ")) {
      modifier |= Modifier.SYNCHRONIZED;
      definition = definition.replace("synchronized ", "");
    }
    // not bothering with strictfp, transient or volatile just yet
    if (definition.contains("abstract ")) {
      modifier |= Modifier.ABSTRACT;
      if (Modifier.isFinal(modifier))
        throw new TypeDefinitionException(
            "A class or method cannot be both abstract and final!");
      if (Modifier.isNative(modifier))
        throw new TypeDefinitionException(
            "A method cannot be both abstract and native!");
      // can't check static until we know whether we're parsing a class or a
      // method
      definition = definition.replace("abstract ", "");
    }

    this.modifier = modifier;

    int index;

    if (definition.contains("interface ")) {
      definition = definition.replace("interface ", "");
      isClass = false;
      superClass = null;
      // extends applies to superinterfaces
      index = definition.indexOf("extends ");
      if (index > 0) {
        for (String iface : definition.substring(index + 8).split(",")) {
          iface = iface.trim();
          index = iface.lastIndexOf('.');
          if (index > 0) {
            imports.add(iface);
            iface = iface.substring(index + 1);
          }
          interfaces.add(iface);
        }
        definition = definition.substring(0, index);
      }
    } else {
      isClass = definition.contains("class ");
      if (isClass) {

        definition = definition.replace("class ", "");
        // extends applies to superclass
        index = definition.indexOf("extends ");
        if (index > 0) {
          int endIndex = definition.indexOf(' ', index+8);
          if (endIndex == -1) {
            superClass = definition.substring(index+8);
            definition = definition.replace("extends "+superClass, "");
          } else {
            superClass = definition.substring(index+8, endIndex);
            definition = definition.replace("extends "+superClass + " ", "");
          }
        } else {
          superClass = null;
        }
        index = definition.indexOf("implements ");
        if (index > 0) {
          for (String iface : definition.substring(index + 11).split(",")) {
            iface = iface.trim();
            int period = iface.lastIndexOf('.');
            if (period > 0) {
              // we have to pull generics off this iface as well
              int generic = iface.indexOf('<');
              if (generic == -1) {
                imports.add(iface);
              } else {
                imports.add(iface.substring(0, generic));
              }
              iface = iface.substring(period + 1);
            }
            interfaces.add(iface);
          }
          definition = definition.substring(0, index);
        }
      } else {
        superClass = null;
      }
    }

    index = definition.indexOf('<');
    if (index > -1) {
      int methodLim = definition.indexOf('(');
      if (methodLim < 0 || methodLim > index) {
        isGenerics = true;
        int end = findEnd(definition, index);
        String generic = definition.substring(index + 1, end);
        for (String gen : generic.split(",")) {
          gen = gen.trim();
          boolean noImport = gen.contains("!");
          if (noImport) {
            gen = gen.replaceAll("[!]", "");
          } else {
            for (String part : gen.split(" ")) {
              int period = part.lastIndexOf('.');
              if (period < 0)
                continue;
              imports.add(part);
              gen = gen.replace(part.substring(0, period + 1), "");
            }
          }
          generics.add(gen);
        }
        String prefix = definition.substring(0, index);
        if (end < definition.length() - 1) {
          definition = prefix + definition.substring(end + 1);
        } else
          definition = prefix;
      } else {
        isGenerics = false;
      }
    } else {
      isGenerics = false;
    }

    definition = definition.trim();
    // some runtime validation
    if (definition.contains(" ") && isClass)
      throw new TypeDefinitionException("Found ambiguous class definition in "
          + original + "; leftover: " + definition);
    if (definition.length() == 0)
      throw new TypeDefinitionException(
          "Did not have a class name in class definition " + original);
    if (Modifier.isStatic(modifier) && Modifier.isAbstract(modifier)
        && !isClass)
      throw new TypeDefinitionException(
          "A method cannot be both abstract and static!");
    className = definition;
  }

  private int findEnd(String definition, int index) {
    int opened = 1;
    while (index < definition.length()) {
      switch (definition.charAt(++index)) {
      case '<':
        opened++;
        break;
      case '>':
        if (--opened == 0)
          return index;
      }
    }
    return -1;
  }

  public String getClassName() {
    return className;
  }

  public int getPrivacy() {
    return modifier & 7;// bitmask, so value can do == matching
  }

  public int getModifier() {
    return modifier;
  }

  public String getSuperClass() {
    return superClass;
  }

  public String[] getGenerics() {
    return generics.toArray(new String[generics.size()]);
  }

  public String[] getImports() {
    return imports.toArray(new String[imports.size()]);
  }

  public String[] getInterfaces() {
    return interfaces.toArray(new String[interfaces.size()]);
  }

  public boolean isPublic() {
    return Modifier.isPublic(modifier);
  }

  public boolean isPrivate() {
    return Modifier.isPrivate(modifier);
  }

  public boolean isProtected() {
    return Modifier.isProtected(modifier);
  }

  public boolean isStatic() {
    return Modifier.isStatic(modifier);
  }

  public boolean isFinal() {
    return Modifier.isFinal(modifier);
  }

  public boolean isAbstract() {
    return Modifier.isAbstract(modifier);
  }

  public boolean isNative() {
    return Modifier.isNative(modifier);
  }

  public boolean isClass() {
    return isClass;
  }

  public boolean hasGenerics() {
    return isGenerics;
  }

  public static Iterable<String> findImportsInGeneric(final String generic) {
    return new Iterable<String>() {
      class Itr implements Iterator<String> {

        int pos = 0, max = generic.length();
        String qualifiedName;
        @Override
        public boolean hasNext() {
          while (pos < max) {
            if (Character.isJavaIdentifierStart(generic.charAt(pos))){
              // do we have a period before anything else?
              int start = pos;
              while (Character.isJavaIdentifierPart(generic.charAt(pos)))
                if (++pos == max)
                  return false;
              int whitespace = eatWhitespace(generic, pos);
              if (generic.charAt(whitespace) == '.') {
                // we have a fqcn.  Let's eat it.
                StringBuilder b = new StringBuilder(generic.substring(start, pos));
                pos = whitespace;
                do{
                  start = pos = eatWhitespace(generic, pos+1);
                  while (Character.isJavaIdentifierPart(generic.charAt(pos)))
                    if (++pos == max) {
                      break;
                    }
                  b.append('.').append(generic.substring(start, pos));
                  pos = eatWhitespace(generic, pos);
                  if (pos==max)break;
                } while(generic.charAt(pos) == '.');
                qualifiedName = b.toString();
                return true;
              }
            }
            else {
              pos ++;
            }
          }
          return false;
        }

        @Override
        public String next() {
          return qualifiedName;
        }

        @Override
        public void remove() {}

      }
      @Override
      public Iterator<String> iterator() {
        return new Itr();
      }
    };
  }

  protected static String stripTypeMods(String type) {
    int end = type.indexOf('<');
    if (end != -1)
      type = type.substring(0, end);
    end = type.indexOf('[');
    if (end != -1)
      type = type.substring(0, end);
    return type;
  }

  protected static int lexType(final IsType into, CharSequence chars, int pos) {
    int start = pos = eatWhitespace(chars, pos);
    int lastPeriod = -1, max = chars.length()-1;
    boolean doneParsing = false;
    StringBuilder pkg = new StringBuilder();
    package_loop:
    if (Character.isLowerCase(chars.charAt(pos))) {
      // We may have package names to read.
      while(true) {
        pos = eatWhitespace(chars, pos);
        while(Character.isJavaIdentifierPart(chars.charAt(++pos))){
          if (pos == max){
            if (lastPeriod == -1) {
              // no package, so don't go looking for class references either.
              doneParsing = true;
            } else {
              // everything up to the last period is already in package variable.
              start = pos = lastPeriod + 1;
            }
            break package_loop;
          }
        }
        int whitespace = eatWhitespace(chars, pos);
        if (chars.charAt(whitespace) == '.') {
          if (whitespace > pos && chars.charAt(whitespace+1)=='.') {
            break package_loop;
          }
          if (lastPeriod != -1)
            pkg.append('.');
          lastPeriod = pos = whitespace;
          pkg.append(chars.subSequence(start, pos).toString().trim());
          pos = start = eatWhitespace(chars, pos+1);
          if (Character.isUpperCase(chars.charAt(start))) {
            break package_loop;
          }
        } else {
          if (whitespace > pos) {
            doneParsing = true;
            break package_loop;
          }
        }
      }
  }
    if (doneParsing){
      if (pos == max) {
        into.setType(pkg.toString(), chars.subSequence(start, pos+1).toString());
        return pos;
      }
      into.setType(pkg.toString(), chars.subSequence(start, pos).toString());
    } else {
      StringBuilder typeName = new StringBuilder();
      lastPeriod = -1;
      typeloop:
      while (true) {
        pos = eatWhitespace(chars, pos);
        if (!Character.isJavaIdentifierStart(chars.charAt(pos))){
          if (pos > start) {
            if (lastPeriod != -1)
              typeName.append('.');
            typeName.append(chars.subSequence(start, pos).toString().trim());
          }
          break;
        }
        while(Character.isJavaIdentifierPart(chars.charAt(++pos)))
          if (pos == max) {
            if (lastPeriod != -1)
              typeName.append('.');
            typeName.append(chars.subSequence(start, pos+1).toString().trim());
            break typeloop;
          }
        int whitespace = eatWhitespace(chars, pos);
        if (chars.charAt(whitespace) == '.') {
          if (lastPeriod != -1)
            typeName.append('.');
          if (pos != whitespace && chars.charAt(whitespace + 1) == '.') {
            typeName.append(chars.subSequence(start, pos).toString().trim());
            break;
          }
          lastPeriod = pos = whitespace;
          typeName.append(chars.subSequence(start, pos).toString());
          start = pos = eatWhitespace(chars, pos+1);
        } else {
          if (whitespace > pos) {
            if (lastPeriod != -1)
              typeName.append('.');
            typeName.append(chars.subSequence(start, pos).toString().trim());
            break;
          }
        }
      }
      into.setType(pkg.toString(), typeName.toString());
      if (pos == max)pos++;
    }
    into.packageName = pkg.toString();
    start = pos = eatWhitespace(chars, pos);
    if (pos < chars.length()) {
      pos = eatGeneric(chars, pos);
      if (pos != start)
        lexGenerics(into.generics, chars.subSequence(start, ++pos), 0);
    }
    pos = eatWhitespace(chars, pos);
    try{
      while (chars.charAt(pos) == '[') {
        into.arrayDepth ++;
        while (chars.charAt(++pos)!=']');
        pos = eatWhitespace(chars, pos+1);
      }
    } catch (IndexOutOfBoundsException ignored){}
    if (pos < chars.length() && chars.charAt(pos) == '.') {
      assert chars.charAt(pos+1) == '.';
      assert chars.charAt(pos+2) == '.';
      into.arrayDepth++;
      pos = eatWhitespace(chars, pos+3);
    }
    return pos;
  }

  private static void lexGenerics(
      SimpleStack<IsGeneric> into, CharSequence chars, int pos) {

  }

  public static IsParameter lexParam(CharSequence chars) {
    int pos = eatWhitespace(chars, 0);
    HasModifier mods = new HasModifier();
    HasAnnotations annos = new HasAnnotations();
    pos = visitModifier(new ModifierExtractor(), mods, chars, pos);
    pos = visitAnnotation(new AnnotationExtractor(), annos, chars, pos);
    TypeDef type = extractType(chars, pos);
    int start = eatWhitespace(chars, type.index);
    pos = eatJavaname(chars, start);
    IsParameter param = new IsParameter(chars.subSequence(start, pos).toString(), type.toString());
    param.annotations = annos;
    return param;
  }

  public static <Param> void visitClassFile(ClassVisitor<Param> extractor,
      Param receiver, CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    // Check for copyright
    if (chars.charAt(pos) == '/') {
      StringBuilder b = new StringBuilder();
      pos = visitJavadoc(new JavadocExtractor(), b, chars, pos);
      extractor.visitCopyright(b.toString(), receiver);
      pos = eatWhitespace(chars, pos);
    }
    // Grab package, if it exists
    if (chars.charAt(pos) == 'p') {
      // maybe package statement
      if (chars.charAt(pos+1) == 'a') {
        assert chars.subSequence(pos, pos+7).toString().equals("package");
        int start = pos = eatWhitespace(chars, pos+7);
        pos = eatJavaname(chars, pos);
        extractor.visitPackage(chars.subSequence(start, pos).toString(), receiver);
        pos = eatWhitespace(chars, pos);
        assert chars.charAt(pos) == ';';
        pos = eatWhitespace(chars, pos+1);
      }
      // Grab imports
      while (chars.charAt(pos) == 'i') {
        // might be "interface"
        if (chars.charAt(pos+1)=='n')
          break;
        assert chars.subSequence(pos, pos+6).toString().endsWith("import");
        int start = pos = eatWhitespace(chars, pos+6);
        boolean isStatic = false;
        pos = eatJavaname(chars, pos);
        String value = chars.subSequence(start, pos).toString();
        if ("static".equals(value)) {
          isStatic = true;
          start = pos = eatWhitespace(chars, pos);
          pos = eatJavaname(chars, pos);
        }
        if (chars.charAt(pos) == '*')
          ++pos;
        extractor.visitImport(chars.subSequence(start, pos).toString(), isStatic, receiver);
        pos = eatWhitespace(chars, pos);
        assert chars.charAt(pos)==';';
        // Not allowed to have multiple ; here
        pos = eatWhitespace(chars, pos+1);
      }

      // Visit class definition itself
    }
  }

}

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
import xapi.source.read.JavaVisitor.ClassBodyVisitor;
import xapi.source.read.JavaVisitor.ClassVisitor;
import xapi.source.read.JavaVisitor.GenericVisitor;
import xapi.source.read.JavaVisitor.JavadocVisitor;
import xapi.source.read.JavaVisitor.MethodVisitor;
import xapi.source.read.JavaVisitor.ModifierVisitor;
import xapi.source.read.JavaVisitor.ParameterVisitor;
import xapi.source.read.JavaVisitor.TypeData;

@SuppressWarnings("rawtypes")
public class JavaLexer {

  public static class ModifierExtractor implements ModifierVisitor<HasModifier> {

    @Override
    public void visitModifier(final int modifier, final HasModifier receiver) {
      receiver.modifier |= modifier;
    }

  }
  public static class AnnotationExtractor implements AnnotationVisitor<HasAnnotations> {

    @Override
    public AnnotationMemberVisitor<HasAnnotations> visitAnnotation(final String annoName, final String annoBody, final HasAnnotations receiver) {
      final IsAnnotation anno = new IsAnnotation(annoName);
      if (receiver != null) {
        receiver.addAnnotation(anno);
      }
      return new AnnotationMemberExtractor();
    }
  }

  public static class JavadocExtractor implements JavadocVisitor<StringBuilder> {
    @Override
    public void visitJavadoc(final String javadoc, final StringBuilder receiver) {
      // TODO: remove *s
      receiver.append(javadoc);
    }
  }

  public static class AnnotationMemberExtractor implements AnnotationMemberVisitor<HasAnnotations> {

    @Override
    public void visitMember(final String name, final String value, final HasAnnotations receiver) {
      assert !receiver.annotations.isEmpty() :
        "You must visit an annotation before visiting an annotation member";
      receiver.annotations.tail().members.add(new AnnotationMember(name, value));
    }

  }

  public static class GenericsExtractor implements GenericVisitor<SimpleStack<IsGeneric>> {
    @Override
    public void visitGeneric(String generic, final SimpleStack<IsGeneric> receiver) {
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

    public TypeDef(final String name) {
      super(name);
    }

    public TypeDef(final String name, final int index) {
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

    protected MemberData(final int modifier, final String simpleName, final String typeName,
        final String javaDoc) {
      this.modifier = modifier;
      this.simpleName = simpleName;
      this.typeName = typeName;
      this.javaDoc = javaDoc;
      generics = new TreeSet<String>();
      imports = new TreeSet<String>();
      annotations = new TreeSet<String>();
    }
  }

  private static final ModifierVisitor NO_OP_MOD_VISITOR = new ModifierVisitor() {
    @Override
    public void visitModifier(final int modifier, final Object receiver) {
    }
  };

  private static final GenericVisitor NO_OP_GENERIC_VISITOR = new GenericVisitor() {
    @Override
    public void visitGeneric(final String generic, final Object receiver) {}
  };

  private static final ClassVisitor NP_OP_CLASS_VISITOR = new ClassVisitor() {

    @Override
    public AnnotationMemberVisitor visitAnnotation(final String annoName, final String annoBody, final Object receiver) {
      return null;
    }

    @Override
    public void visitGeneric(final String generic, final Object receiver) {
    }

    @Override
    public void visitJavadoc(final String javadoc, final Object receiver) {
    }

    @Override
    public void visitModifier(final int modifier, final Object receiver) {
    }

    @Override
    public void visitImport(final String name, final boolean isStatic, final Object receiver) {
    }

    @Override
    public void visitCopyright(final String copyright, final Object receiver) {
    }

    @Override
    public void visitPackage(final String pkg, final Object receiver) {
    }

    @Override
    public void visitName(final String name, final Object receiver) {
    }

    @Override
    public void visitType(final String type, final Object receiver) {
    }

    @Override
    public void visitSuperclass(final String superClass, final Object receiver) {
    }

    @Override
    public void visitInterface(final String iface, final Object receiver) {
    }

    @Override
    public ClassBodyVisitor visitBody(final String body, final Object receiver) {
      return null;
    }
  };

  protected static final AnnotationMemberVisitor NO_OP_ANNOTATION_MEMBER_VISITOR = new AnnotationMemberVisitor() {
    @Override
    public void visitMember(final String name, final String value, final Object receiver) {
    }
  };

  private static final AnnotationVisitor NO_OP_ANNOTATION_VISITOR = new AnnotationVisitor() {
    @Override
    public AnnotationMemberVisitor visitAnnotation(final String annoName, final String annoBody, final Object receiver) {
      return NO_OP_ANNOTATION_MEMBER_VISITOR;
    }
  };

  public static <R> int visitJavadoc
  (final JavadocVisitor<R> visitor, final R receiver, final CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (pos == chars.length()) {
      return pos;
    }
    try {
      if ('/' == chars.charAt(pos)) {
        if (chars.charAt(++pos) == '*') {
          final int start = pos + (chars.charAt(pos) == '*' ? 1 : 0);
          // We hava some javadoc. Let's eat it all
          do {
            while (chars.charAt(++pos) != '*') {
              ;
            }
          } while (chars.charAt(++pos) != '/');
          chars.subSequence(start, pos - 2);
          visitor.visitJavadoc(chars.toString().replaceAll("\n\\s*[*]", "")
              // eat opening \n * javadoc chars
              , receiver);
        }
      }
    } catch (final IndexOutOfBoundsException e) {
      error(e, "Error parsing javadoc on: " + chars.toString());
    }
    return pos;
  }

  public static <R> int visitAnnotation
  (final AnnotationVisitor<R> visitor, final R receiver, final CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (pos == chars.length()) {
      return pos;
    }
    int start = pos;
    try {
      while(chars.charAt(pos) == '@') {
        pos = eatJavaname(chars, pos + 1);
        final String annoName = chars.subSequence(start + 1, pos).toString();
        String annoBody = "";
        pos = eatWhitespace(chars, pos);
        if (pos < chars.length() && chars.charAt(pos) == '(') {
          // Annotation has a body
          final int bodyStart = pos+1;
          pos = eatAnnotationBody(visitor, receiver, chars, pos);
          annoBody = chars.subSequence(bodyStart, pos).toString();
          pos ++;
        }
        final AnnotationMemberVisitor<R> bodyVisitor = visitor.visitAnnotation(annoName, annoBody, receiver);
        if (bodyVisitor != null && annoBody.length() > 0) {
          visitAnnotationMembers(bodyVisitor, receiver, annoBody, 0);
        }
        start = pos = eatWhitespace(chars, pos);
        if (pos == chars.length()) {
          break;
        }
      }
    } catch (final IndexOutOfBoundsException e) {
      error(
          e,
          "Error parsing annotation on: "
              + chars.subSequence(start, chars.length()));
    }
    return pos;
  }

  public static <R> int visitAnnotationMembers
  (final AnnotationMemberVisitor<R> visitor, final R receiver, final CharSequence chars, int pos) {
    String name = "value";
    boolean nameNext = true;
    while (true) {
      if (pos == chars.length()) {
        return pos;
      }
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
          final int start = pos;
          while (Character.isJavaIdentifierPart(chars.charAt(pos))) {
            pos++;
          }
          final String maybeName = chars.subSequence(start, pos).toString().trim();
          if (maybeName.length() == 0) {
            name = "value";
          } else {
            name = maybeName;
          }
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
          final int start = pos;
          switch (chars.charAt(pos)) {
          case '{':
            pos = eatArrayInitializer(chars, pos);

            break;
          case '"':
            pos = eatStringValue(chars, pos);
            if (chars.charAt(pos) == '"') {
              pos++;
            }
            break;
          case '@':
            final AnnotationVisitor<HasAnnotations> extractor = new AnnotationExtractor();
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
  (final ModifierVisitor<R> visitor, final R receiver, final CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    while(true) {

      final char c = chars.charAt(pos);
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
      case 'd':
        // default
        if (chars.subSequence(pos, pos+8).equals("default ")) {
          pos = eatWhitespace(chars, pos+8);
          visitor.visitModifier(JavaVisitor.MODIFIER_DEFAULT, receiver);
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
  (final GenericVisitor<R> visitor, final R receiver,
      final CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (chars.charAt(pos) == '<') {
      final int start = pos;
      pos = eatGeneric(chars, pos)+1;
      visitor.visitGeneric(chars.subSequence(start, pos).toString(), receiver);
    }
    return eatWhitespace(chars, pos);
  }

  public static <R> int visitMethodSignature
  (final MethodVisitor<R> visitor, final R receiver, final CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (pos == chars.length()) {
      return pos;
    }

    pos = visitAnnotation(visitor, receiver, chars, pos);
    pos = visitModifier(visitor, receiver, chars, pos);
    pos = visitGeneric(visitor, receiver, chars, pos);
    final TypeDef returnType = extractType(chars, pos);
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
        final ParameterVisitor<R> param = visitor.visitParameter();
        pos = visitAnnotation(param, receiver, chars, pos);
        pos = visitModifier(param, receiver, chars, pos);

        final TypeDef def = extractType(chars, pos);
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
        if (chars.charAt(pos) == ',') {
          pos++;
        }
      }
    }
    if (pos == chars.length()) {
      return pos;
    }
    pos = eatWhitespace(chars, pos+1);
    if (pos == chars.length()) {
      return pos;
    }
    // exceptions
    if (chars.charAt(pos) == 't') {
      if (chars.subSequence(pos, pos+6).equals("throws")) {
        pos = eatWhitespace(chars, pos+7);
        while (pos < chars.length()) {
          if (chars.charAt(pos)=='{' || chars.charAt(pos) == ';') {
            return pos;
          }
          start = pos;
          pos = eatJavaname(chars, pos);
          visitor.visitException(chars.subSequence(start, pos).toString(), receiver);
          pos = eatWhitespace(chars, pos);
          if (pos == chars.length()) {
            return pos;
          }
          if (chars.charAt(pos) == ',') {
            pos = eatWhitespace(chars, pos+1);
          }
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
  public static TypeDef extractType(final CharSequence chars, int pos)
  {
    int start = pos = eatWhitespace(chars, pos);
    int lastPeriod = -1;
    final int max = chars.length()-1;
    boolean doneParsing = false;
    final StringBuilder pkg = new StringBuilder();
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
        final int whitespace = eatWhitespace(chars, pos);
        final int next = chars.charAt(whitespace);
        if (next == '.') {
          if (whitespace > pos && chars.charAt(whitespace+1)=='.') {
            break package_loop;
          }
          if (lastPeriod != -1) {
            pkg.append('.');
          }
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
      if (pos == max) {
        return new TypeDef(chars.subSequence(start, pos+1).toString(), pos);
      }
      def = new TypeDef(chars.subSequence(start, pos).toString());
    } else {
      final StringBuilder typeName = new StringBuilder();
      lastPeriod = -1;
      typeloop:
      while (true) {
        pos = eatWhitespace(chars, pos);
        if (!Character.isJavaIdentifierStart(chars.charAt(pos))){
          if (pos > start) {
            if (lastPeriod != -1) {
              typeName.append('.');
            }
            typeName.append(chars.subSequence(start, pos).toString().trim());
          }
          break;
        }
        while(Character.isJavaIdentifierPart(chars.charAt(++pos))) {
          if (pos == max) {
            if (lastPeriod != -1) {
              typeName.append('.');
            }
            typeName.append(chars.subSequence(start, pos+1).toString().trim());
            break typeloop;
          }
        }
        final int whitespace = eatWhitespace(chars, pos);
        if (chars.charAt(whitespace) == '.') {
          if (lastPeriod != -1) {
            typeName.append('.');
          }
          if (pos != whitespace && chars.charAt(whitespace + 1) == '.') {
            typeName.append(chars.subSequence(start, pos).toString().trim());
            break;
          }
          lastPeriod = pos = whitespace;
          typeName.append(chars.subSequence(start, pos).toString());
          start = pos = eatWhitespace(chars, pos+1);
        } else {
          if (whitespace > pos) {
            if (lastPeriod != -1) {
              typeName.append('.');
            }
            typeName.append(chars.subSequence(start, pos).toString().trim());
            break;
          }
        }
      }
      def = new TypeDef(typeName.toString());
      if (pos == max) {
        pos++;
      }
    }
    def.pkgName = pkg.toString();
    start = pos = eatWhitespace(chars, pos);
    if (pos < chars.length()) {
      pos = eatGeneric(chars, pos);
      if (pos != start) {
        def.generics = chars.subSequence(start, ++pos).toString();
      }
    }
    pos = eatWhitespace(chars, pos);
    while (pos < max && chars.charAt(pos) == '[') {
      def.arrayDepth ++;
      while (chars.charAt(++pos)!=']') {
        ;
      }
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

  public static SimpleStack<IsGeneric> extractGenerics (final CharSequence chars, final int pos) {
    final SimpleStack<IsGeneric> stack = new SimpleStack<IsGeneric>();
    visitGeneric(new GenericsExtractor(), stack, chars, pos);
    return stack;
  }

  protected static <R> int eatAnnotationBody
  (final AnnotationVisitor<R> visitor,final R receiver, final CharSequence chars, int pos) {
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
          while (Character.isJavaIdentifierPart(chars.charAt(pos))) {
            pos++;
          }
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

  protected static <R > int eatWhitespaceAndComments(final CharSequence chars, final int pos) {
    final int next = eatWhitespace(chars, pos);
    final int comment = eatComments(chars, next);
    if (comment == pos) {
      return pos;
    }
    return eatWhitespaceAndComments(chars, comment);
  }

  protected static int eatComments(final CharSequence chars, int pos) {
    if (chars.charAt(pos)=='/') {
      if (chars.charAt(pos+1)=='/') {
        // go to the newline
        while (chars.charAt(++pos) != '\n') {
          ;
        }
        pos++;
      } else if (chars.charAt(pos+1)== '*') {
        // go to the */
        boolean done = false;
        while (!done) {
          while (chars.charAt(++pos) != '*') {}
          done = chars.charAt(++pos) == '/';
        }
        pos++;
      }
    }
    return pos;
  }

  protected static <R > int eatWhitespace(final CharSequence chars, int pos) {
    try {
      while (Character. isWhitespace(chars .charAt(pos))) {
        pos++;
      }
    } catch (final IndexOutOfBoundsException ignored) {
    }
    return pos;
  }

  protected static int eatJavaname(final CharSequence chars, int pos) {
    try {
      while (Character.isJavaIdentifierPart(chars.charAt(pos))) {
        pos++;
      }
      if (chars.charAt(pos) == '.') {
        return eatJavaname(chars, pos + 1);
      }
    } catch (final IndexOutOfBoundsException ignored) {}
    return pos;
  }

  protected static <R> int eatGeneric(final CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    if (pos == chars.length()) {
      return pos;
    }
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

  protected static int eatStringValue(final CharSequence chars, int pos) {
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
        while ((c = chars.charAt(++pos)) != '"' || escaped) {
          escaped = c == '\\' && !escaped;
        }
      }
    } catch (final IndexOutOfBoundsException e) {

    }
    return pos;
  }

  protected static int eatArrayValue(final CharSequence chars, int pos) {
    int arrayDepth = 1;
    while (arrayDepth > 0) {
      final char c = chars.charAt(++pos);
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

  protected static int eatArrayInitializer(final CharSequence chars, int pos) {
    while (true) {
      final char c = chars.charAt(++pos);
      if (c == '"') {
        pos = eatStringValue(chars, pos);
      } else if (c == '}') {
        return pos;
      }
    }
  }

  protected static boolean isQualified(final String typeName) {
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

  protected static void error(final Throwable e, final String string) {
    if (string != null) {
      System.err.println(string);
    }
    if (e != null) {
      e.printStackTrace();
    }
  }

  private final int modifier;

  private final boolean isClass;
  private final boolean isAnnotation;
  private final boolean isEnum;
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
    final String original = definition;
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
      if (Modifier.isFinal(modifier)) {
        throw new TypeDefinitionException(
            "A class or method cannot be both abstract and final!");
      }
      if (Modifier.isNative(modifier)) {
        throw new TypeDefinitionException(
            "A method cannot be both abstract and native!");
      }
      // can't check static until we know whether we're parsing a class or a
      // method
      definition = definition.replace("abstract ", "");
    }

    this.modifier = modifier;

    int index;

    if (definition.contains("interface ")) {
      isEnum = false;
      isAnnotation = definition.contains("@interface");
      if (isAnnotation) {
        definition = definition.replace("@interface ", "");
      } else {
        definition = definition.replace("interface ", "");
      }
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
        isEnum = false;
      } else {
        isEnum = definition.contains("enum ");
        definition = definition.replace("enum ", "");
      }
      isAnnotation = false;
      if (isClass) {

        // extends applies to superclass
        index = definition.indexOf("extends ");
        if (index > 0) {
          final int endIndex = definition.indexOf(' ', index+8);
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
            final int period = iface.lastIndexOf('.');
            if (period > 0) {
              // we have to pull generics off this iface as well
              final int generic = iface.indexOf('<');
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
      final int methodLim = definition.indexOf('(');
      if (methodLim < 0 || methodLim > index) {
        isGenerics = true;
        final int end = findEnd(definition, index);
        final String generic = definition.substring(index + 1, end);
        for (String gen : generic.split(",")) {
          gen = gen.trim();
          final boolean noImport = gen.contains("!");
          if (noImport) {
            gen = gen.replaceAll("[!]", "");
          } else {
            for (final String part : gen.split(" ")) {
              final int period = part.lastIndexOf('.');
              if (period < 0) {
                continue;
              }
              imports.add(part);
              gen = gen.replace(part.substring(0, period + 1), "");
            }
          }
          generics.add(gen);
        }
        final String prefix = definition.substring(0, index);
        if (end < definition.length() - 1) {
          definition = prefix + definition.substring(end + 1);
        } else {
          definition = prefix;
        }
      } else {
        isGenerics = false;
      }
    } else {
      isGenerics = false;
    }

    definition = definition.trim();
    // some runtime validation
    if (definition.contains(" ") && isClass) {
      throw new TypeDefinitionException("Found ambiguous class definition in "
          + original + "; leftover: " + definition);
    }
    if (definition.length() == 0) {
      throw new TypeDefinitionException(
          "Did not have a class name in class definition " + original);
    }
    if (Modifier.isStatic(modifier) && Modifier.isAbstract(modifier)
        && !isClass) {
      throw new TypeDefinitionException(
          "A method cannot be both abstract and static!");
    }
    className = definition;
  }

  private int findEnd(final String definition, int index) {
    int opened = 1;
    while (index < definition.length()) {
      switch (definition.charAt(++index)) {
      case '<':
        opened++;
        break;
      case '>':
        if (--opened == 0) {
          return index;
        }
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

  public boolean isAnnotation() {
    return isAnnotation;
  }

  public boolean isEnum() {
    return isEnum;
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
              while (Character.isJavaIdentifierPart(generic.charAt(pos))) {
                if (++pos == max) {
                  return false;
                }
              }
              final int whitespace = eatWhitespace(generic, pos);
              if (generic.charAt(whitespace) == '.') {
                // we have a fqcn.  Let's eat it.
                final StringBuilder b = new StringBuilder(generic.substring(start, pos));
                pos = whitespace;
                do{
                  start = pos = eatWhitespace(generic, pos+1);
                  while (Character.isJavaIdentifierPart(generic.charAt(pos))) {
                    if (++pos == max) {
                      break;
                    }
                  }
                  b.append('.').append(generic.substring(start, pos));
                  pos = eatWhitespace(generic, pos);
                  if (pos==max) {
                    break;
                  }
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
    if (end != -1) {
      type = type.substring(0, end);
    }
    end = type.indexOf('[');
    if (end != -1) {
      type = type.substring(0, end);
    }
    return type;
  }

  protected static int lexType(final IsType into, final CharSequence chars, int pos) {
    int start = pos = eatWhitespace(chars, pos);
    int lastPeriod = -1;
    final int max = chars.length()-1;
    boolean doneParsing = false;
    final StringBuilder pkg = new StringBuilder();
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
        final int whitespace = eatWhitespace(chars, pos);
        if (chars.charAt(whitespace) == '.') {
          if (whitespace > pos && chars.charAt(whitespace+1)=='.') {
            break package_loop;
          }
          if (lastPeriod != -1) {
            pkg.append('.');
          }
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
      final StringBuilder typeName = new StringBuilder();
      lastPeriod = -1;
      typeloop:
      while (true) {
        pos = eatWhitespace(chars, pos);
        if (!Character.isJavaIdentifierStart(chars.charAt(pos))){
          if (pos > start) {
            if (lastPeriod != -1) {
              typeName.append('.');
            }
            typeName.append(chars.subSequence(start, pos).toString().trim());
          }
          break;
        }
        while(Character.isJavaIdentifierPart(chars.charAt(++pos))) {
          if (pos == max) {
            if (lastPeriod != -1) {
              typeName.append('.');
            }
            typeName.append(chars.subSequence(start, pos+1).toString().trim());
            break typeloop;
          }
        }
        final int whitespace = eatWhitespace(chars, pos);
        if (chars.charAt(whitespace) == '.') {
          if (lastPeriod != -1) {
            typeName.append('.');
          }
          if (pos != whitespace && chars.charAt(whitespace + 1) == '.') {
            typeName.append(chars.subSequence(start, pos).toString().trim());
            break;
          }
          lastPeriod = pos = whitespace;
          typeName.append(chars.subSequence(start, pos).toString());
          start = pos = eatWhitespace(chars, pos+1);
        } else {
          if (whitespace > pos) {
            if (lastPeriod != -1) {
              typeName.append('.');
            }
            typeName.append(chars.subSequence(start, pos).toString().trim());
            break;
          }
        }
      }
      into.setType(pkg.toString(), typeName.toString());
      if (pos == max) {
        pos++;
      }
    }
    into.packageName = pkg.toString();
    start = pos = eatWhitespace(chars, pos);
    if (pos < chars.length()) {
      pos = eatGeneric(chars, pos);
      if (pos != start) {
        lexGenerics(into.generics, chars.subSequence(start, ++pos), 0);
      }
    }
    pos = eatWhitespace(chars, pos);
    try{
      while (chars.charAt(pos) == '[') {
        into.arrayDepth ++;
        while (chars.charAt(++pos)!=']') {
          ;
        }
        pos = eatWhitespace(chars, pos+1);
      }
    } catch (final IndexOutOfBoundsException ignored){}
    if (pos < chars.length() && chars.charAt(pos) == '.') {
      assert chars.charAt(pos+1) == '.';
      assert chars.charAt(pos+2) == '.';
      into.arrayDepth++;
      pos = eatWhitespace(chars, pos+3);
    }
    return pos;
  }

  private static void lexGenerics(
      final SimpleStack<IsGeneric> into, final CharSequence chars, final int pos) {

  }

  public static IsParameter lexParam(final CharSequence chars) {
    int pos = eatWhitespace(chars, 0);
    final HasModifier mods = new HasModifier();
    final HasAnnotations annos = new HasAnnotations();
    pos = visitModifier(new ModifierExtractor(), mods, chars, pos);
    pos = visitAnnotation(new AnnotationExtractor(), annos, chars, pos);
    final TypeDef type = extractType(chars, pos);
    final int start = eatWhitespace(chars, type.index);
    pos = eatJavaname(chars, start);
    final IsParameter param = new IsParameter(chars.subSequence(start, pos).toString(), type.toString());
    param.annotations = annos;
    param.modifier = mods.modifier;
    return param;
  }

  public static <Param> int visitClassFile(final ClassVisitor<Param> extractor,
      final Param receiver, final CharSequence chars, int pos) {
    pos = eatWhitespace(chars, pos);
    // Check for copyright
    if (chars.charAt(pos) == '/') {
      final StringBuilder b = new StringBuilder();
      pos = visitJavadoc(new JavadocExtractor(), b, chars, pos);
      extractor.visitCopyright(b.toString(), receiver);
      pos = eatWhitespace(chars, pos);
    }
    // Grab package, if it exists
    if (chars.charAt(pos) == 'p') {
      // maybe package statement
      if (chars.charAt(pos+1) == 'a') {
        assert chars.subSequence(pos, pos+7).toString().equals("package");
        final int start = pos = eatWhitespace(chars, pos+7);
        pos = eatJavaname(chars, pos);
        extractor.visitPackage(chars.subSequence(start, pos).toString(), receiver);
        pos = eatWhitespace(chars, pos);
        assert chars.charAt(pos) == ';';
        pos = eatWhitespace(chars, pos+1);
      }
    }
    // Grab imports
    while (chars.charAt(pos) == 'i') {
      // might be "interface"
      if (chars.charAt(pos+1)=='n') {
        break;
      }
      assert chars.subSequence(pos, pos+6).toString().endsWith("import");
      int start = pos = eatWhitespace(chars, pos+6);
      boolean isStatic = false;
      pos = eatJavaname(chars, pos);
      final String value = chars.subSequence(start, pos).toString();
      if ("static".equals(value)) {
        isStatic = true;
        start = pos = eatWhitespace(chars, pos);
        pos = eatJavaname(chars, pos);
      }
      if (chars.charAt(pos) == '*') {
        ++pos;
      }
      extractor.visitImport(chars.subSequence(start, pos).toString(), isStatic, receiver);
      pos = eatWhitespace(chars, pos);
      assert chars.charAt(pos)==';';
      // Not allowed to have multiple ; here
      pos = eatWhitespace(chars, pos+1);
    }
    // Visit class definition itself
    // Start w/ modifiers
    pos = visitModifier(extractor, receiver, chars, pos);
    pos = eatWhitespace(chars, pos);
    // Now check for class / interface / enum / @interface
    boolean isEnum = false, isInterface = false;
    switch(chars.charAt(pos)) {
    case 'c':
      assert chars.subSequence(pos, pos+5).toString().equals("class");
      extractor.visitType(chars.subSequence(pos, pos+5).toString(), receiver);
      pos += 5;
      break;
    case 'i':
      isInterface = true;
      assert chars.subSequence(pos, pos+9).toString().equals("interface");
      extractor.visitType(chars.subSequence(pos, pos+9).toString(), receiver);
      pos += 9;
      break;
    case '@':
      isInterface = true;
      assert chars.subSequence(pos, pos+10).toString().equals("@interface");
      extractor.visitType(chars.subSequence(pos, pos+10).toString(), receiver);
      pos += 10;
      break;
    case 'e':
      isEnum = true;
      assert chars.subSequence(pos, pos+4).toString().equals("enum");
      extractor.visitType(chars.subSequence(pos, pos+4).toString(), receiver);
      pos += 4;
      break;
    }
    pos = eatWhitespace(chars, pos);
    int name = eatJavaname(chars, pos);
    extractor.visitName(chars.subSequence(pos, name).toString(), receiver);
    pos = eatWhitespace(chars, name);
    if (chars.charAt(pos)=='e') {
      // extends
      assert chars.subSequence(pos, pos+7).toString().equals("extends");
      pos += 7;
      pos = eatWhitespace(chars, pos);
      name = eatJavaname(chars, pos);
      name = eatGeneric(chars, name);
      String typeName = chars.subSequence(pos, name).toString();
      if (isInterface) {
        extractor.visitInterface(typeName, receiver);
        pos = eatWhitespace(chars, name+1);
        while (chars.charAt(pos) == ',') {
          pos = eatWhitespace(chars, pos+1);
          name = eatJavaname(chars, pos);
          name = eatGeneric(chars, pos);
          typeName = chars.subSequence(pos, name).toString();
          extractor.visitInterface(typeName, receiver);
          pos = eatWhitespace(chars, name+1);
        }
      } else {
        extractor.visitSuperclass(typeName, receiver);
        pos = eatWhitespace(chars, name+1);
      }
    }
    pos = eatWhitespaceAndComments(chars, pos);
    if (chars.charAt(pos) == 'i') {
      // implements
        assert chars.subSequence(pos, pos+10).toString().equals("implements");
        pos += 10;
        name = eatJavaname(chars, pos);
        name = eatGeneric(chars, name);
        String typeName = chars.subSequence(pos, name).toString();
        extractor.visitInterface(typeName, receiver);
        pos = eatWhitespace(chars, name+1);
        while (chars.charAt(pos) == ',') {
          pos = eatWhitespace(chars, pos+1);
          name = eatJavaname(chars, pos);
          name = eatGeneric(chars, pos);
          typeName = chars.subSequence(pos, name).toString();
          extractor.visitInterface(typeName, receiver);
          pos = eatWhitespace(chars, name+1);
        }
    }
    pos = eatWhitespaceAndComments(chars, pos);
    assert chars.charAt(pos) == '{';
    pos++;
    if (isEnum) {
      pos = eatJavaname(chars, pos);
      pos = eatWhitespaceAndComments(chars, pos);
      while (chars.charAt(pos) != ';' && chars.charAt(pos) != '}') {
        if (chars.charAt(pos) == ',') {
          pos ++;
        }
        pos = eatWhitespaceAndComments(chars, pos);
        pos = eatJavaname(chars, pos);
        pos = eatWhitespaceAndComments(chars, pos);
        if (chars.charAt(pos) == '(') {
          int depth = 1;
          while (depth > 0) {
            pos = eatWhitespace(chars, pos);
            switch (chars.charAt(pos)) {
            case '(':
              depth ++;
              break;
            case ')':
              depth --;
              break;
            case '"':
              pos = eatStringValue(chars, pos);
              break;
            case '{':
              pos = shortcircuitClassBody(chars, pos);
              break;
            }
            pos ++;
          }
        }
        if (chars.charAt(pos) == '{') {
          pos = shortcircuitClassBody(chars, pos+1);
        }
        pos = eatWhitespaceAndComments(chars, pos);
        if (chars.charAt(pos) == ',') {

        }
      }
      if (chars.charAt(pos) == '}') {
        return pos+1;
      }
    }
    pos = eatWhitespaceAndComments(chars, pos+1);

    return shortcircuitClassBody(chars, pos);
  }

  @SuppressWarnings("unchecked")
  protected static int shortcircuitClassBody(final CharSequence chars, int pos) {
 // Short-circuit for now; going to skip over fields, methods and inner types.
    while (chars.charAt(pos) != '}') {
      pos = eatWhitespaceAndComments(chars, pos);
      while (chars.charAt(pos) == '@') {
        System.out.println(chars.charAt(pos)+"\n"+chars.subSequence(pos, pos + 10));
        pos = eatJavaname(chars, pos+1);
        if (chars.charAt(pos)=='(') {
          pos = eatAnnotationBody(NO_OP_ANNOTATION_VISITOR, null, chars, pos);
        }
        pos = eatWhitespaceAndComments(chars, pos);
        System.out.println(chars.charAt(pos)+"\n"+chars.subSequence(pos, pos + 10)+"\n\n\n");
      }
      pos = visitModifier(NO_OP_MOD_VISITOR, null, chars, pos);
      pos = eatJavaname(chars, pos);
      pos = eatWhitespaceAndComments(chars, pos+1);
      pos = visitGeneric(NO_OP_GENERIC_VISITOR, null, chars, pos);
      pos = eatWhitespaceAndComments(chars, pos+1);
      pos = eatJavaname(chars, pos);
      pos = eatWhitespaceAndComments(chars, pos);
      final char c = chars.charAt(pos);
      switch(c) {
      case ';':
        // end of a field declaration, we can continue;
        pos = eatWhitespaceAndComments(chars, pos+1);
        continue;
      case '=':
        // beginning of a field initializer.  Eat statements until we hit a ;
        pos = eatStatement(chars, pos+1);
        pos = eatWhitespaceAndComments(chars, pos);
        break;
      case '(':
        // beginning of a method definition
        // fast forward to { or ;
        while(chars.charAt(++pos)!=')') {
          ;
        }
        pos = eatWhitespace(chars, pos+1);
        if (chars.charAt(pos)==';') {
          continue;
        }
      case 's':
        if (chars.charAt(pos)=='s') {
          assert chars.subSequence(pos, pos+6).toString().equals("static");
          pos = pos+7;
        }
        pos = eatWhitespaceAndComments(chars, pos);
        assert chars.charAt(pos) == '{';
        while (chars.charAt(pos) != '}') {
          pos = eatStatement(chars, pos+1);
          pos = eatWhitespaceAndComments(chars, pos);
        }
        pos++;
        // We are either in a method block or a static block... eat statements until we hit }
        break;
      case 'e':
      case 'i':
        // beginning of an extends or implements; fast forward to {
        while (chars.charAt(++pos)!='{') {
          ;
        }
      case '{':
        // beginning of an inner type
        pos = visitClassFile(NP_OP_CLASS_VISITOR, null, chars, pos);
        break;
      default:
        System.err.println("Unhandled char: "+c+" @ "+chars.subSequence(pos, Math.min(pos+30, chars.length())));
      }
      pos = eatWhitespaceAndComments(chars, pos);
    }
    return pos+1;
  }

  protected static int eatStatement(final CharSequence chars, int pos) {
    pos = eatWhitespaceAndComments(chars, pos);
    while (chars.charAt(pos) != ';') {
      if (chars.charAt(pos) == '"') {
        // eat quoted strings
        pos = eatStringValue(chars, pos);
      } else if (chars.charAt(pos) == '/') {
        pos = eatComments(chars, pos);
      } else if (chars.charAt(pos) == '{') {
        pos = shortcircuitClassBody(chars, pos);
      }
      pos++;
    }
    return pos+1;
  }

}

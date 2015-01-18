/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * MODIFIED BY James Nelson of We The Internet, 2013.
 * Repackaged to avoid conflicts with different versions of Javassist,
 * and modified Javassist APIs to make them more accessible to outside code.
 */
package xapi.bytecode.annotation;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.Map;

import xapi.bytecode.ConstPool;
import xapi.bytecode.attributes.AttributeInfo;
import xapi.util.X_Byte;

public class AnnotationsAttribute extends AttributeInfo {
  /**
   * The name of the <code>RuntimeVisibleAnnotations</code> attribute.
   */
  public static final String visibleTag = "RuntimeVisibleAnnotations";

  /**
   * The name of the <code>RuntimeInvisibleAnnotations</code> attribute.
   */
  public static final String invisibleTag = "RuntimeInvisibleAnnotations";

  /**
   * Constructs a <code>Runtime(In)VisibleAnnotations_attribute</code>.
   *
   * @param cp            constant pool
   * @param attrname      attribute name (<code>visibleTag</code> or
   *                      <code>invisibleTag</code>).
   * @param info          the contents of this attribute.  It does not
   *                      include <code>attribute_name_index</code> or
   *                      <code>attribute_length</code>.
   */
  public AnnotationsAttribute(ConstPool cp, String attrname, byte[] info) {
      super(cp, attrname, info);
  }

  /**
   * Constructs an empty
   * <code>Runtime(In)VisibleAnnotations_attribute</code>.
   * A new annotation can be later added to the created attribute
   * by <code>setAnnotations()</code>.
   *
   * @param cp            constant pool
   * @param attrname      attribute name (<code>visibleTag</code> or
   *                      <code>invisibleTag</code>).
   * @see #setAnnotations(Annotation[])
   */
  public AnnotationsAttribute(ConstPool cp, String attrname) {
      this(cp, attrname, new byte[] { 0, 0 });
  }

  /**
   * @param n     the attribute name.
   */
  public AnnotationsAttribute(ConstPool cp, int n, DataInput in)
      throws IOException
  {
      super(cp, n, in);
  }

  /**
   * Returns <code>num_annotations</code>.
   */
  public int numAnnotations() {
      return X_Byte.readU16bit(info, 0);
  }

  /**
   * Copies this attribute and returns a new copy.
   */
  @Override
  public AttributeInfo copy(ConstPool newCp, Map<?, ?> classnames) {
      Copier copier = new Copier(info, constPool, newCp, classnames);
      try {
          copier.annotationArray();
          return new AnnotationsAttribute(newCp, getName(), copier.close());
      }
      catch (Exception e) {
          throw new RuntimeException(e.toString());
      }
  }

  /**
   * Parses the annotations and returns a data structure representing
   * the annotation with the specified type.  See also
   * <code>getAnnotations()</code> as to the returned data structure.
   *
   * @param type      the annotation type.
   * @return null if the specified annotation type is not included.
   * @see #getAnnotations()
   */
  public Annotation getAnnotation(String type) {
      Annotation[] annotations = getAnnotations();
      for (int i = 0; i < annotations.length; i++) {
          if (annotations[i].getTypeName().equals(type)) {
            return annotations[i];
          }
      }

      return null;
  }

  /**
   * Adds an annotation.  If there is an annotation with the same type,
   * it is removed before the new annotation is added.
   *
   * @param annotation        the added annotation.
   */
  public void addAnnotation(Annotation annotation) {
      String type = annotation.getTypeName();
      Annotation[] annotations = getAnnotations();
      for (int i = 0; i < annotations.length; i++) {
          if (annotations[i].getTypeName().equals(type)) {
              annotations[i] = annotation;
              setAnnotations(annotations);
              return;
          }
      }

      Annotation[] newlist = new Annotation[annotations.length + 1];
      System.arraycopy(annotations, 0, newlist, 0, annotations.length);
      newlist[annotations.length] = annotation;
      setAnnotations(newlist);
  }

  /**
   * Parses the annotations and returns a data structure representing
   * that parsed annotations.  Note that changes of the node values of the
   * returned tree are not reflected on the annotations represented by
   * this object unless the tree is copied back to this object by
   * <code>setAnnotations()</code>.
   *
   * @see #setAnnotations(Annotation[])
   */
  public Annotation[] getAnnotations() {
      try {
          return new Parser(info, constPool).parseAnnotations();
      }
      catch (Exception e) {
          throw new RuntimeException(e.toString());
      }
  }

  /**
   * Changes the annotations represented by this object according to
   * the given array of <code>Annotation</code> objects.
   *
   * @param annotations           the data structure representing the
   *                              new annotations.
   */
  public void setAnnotations(Annotation[] annotations) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      AnnotationsWriter writer = new AnnotationsWriter(output, constPool);
      try {
          int n = annotations.length;
          writer.numAnnotations(n);
          for (int i = 0; i < n; ++i) {
            annotations[i].write(writer);
          }

          writer.close();
      }
      catch (IOException e) {
          throw new RuntimeException(e);      // should never reach here.
      }

      set(output.toByteArray());
  }

  /**
   * Changes the annotations.  A call to this method is equivalent to:
   * <ul><pre>setAnnotations(new Annotation[] { annotation })</pre></ul>
   *
   * @param annotation    the data structure representing
   *                      the new annotation.
   */
  public void setAnnotation(Annotation annotation) {
      setAnnotations(new Annotation[] { annotation });
  }

  /**
   * Returns a string representation of this object.
   */
  @Override
  public String toString() {
      Annotation[] a = getAnnotations();
      StringBuffer sbuf = new StringBuffer();
      int i = 0;
      while (i < a.length) {
          sbuf.append(a[i++].toString());
          if (i != a.length) {
            sbuf.append(", ");
          }
      }

      return sbuf.toString();
  }

 public static class Walker {
      byte[] info;

      Walker(byte[] attrInfo) {
          info = attrInfo;
      }

      public final void parameters() throws Exception {
          int numParam = info[0] & 0xff;
          parameters(numParam, 1);
      }

      public void parameters(int numParam, int pos) throws Exception {
          for (int i = 0; i < numParam; ++i) {
            pos = annotationArray(pos);
          }
      }

      public final void annotationArray() throws Exception {
          annotationArray(0);
      }

      final int annotationArray(int pos) throws Exception {
          int num = X_Byte.readU16bit(info, pos);
          return annotationArray(pos + 2, num);
      }

      int annotationArray(int pos, int num) throws Exception {
          for (int i = 0; i < num; ++i) {
            pos = annotation(pos);
          }

          return pos;
      }

      final int annotation(int pos) throws Exception {
          int type = X_Byte.readU16bit(info, pos);
          int numPairs = X_Byte.readU16bit(info, pos + 2);
          return annotation(pos + 4, type, numPairs);
      }

      int annotation(int pos, int type, int numPairs) throws Exception {
          for (int j = 0; j < numPairs; ++j) {
            pos = memberValuePair(pos);
          }

          return pos;
      }

      final int memberValuePair(int pos) throws Exception {
          int nameIndex = X_Byte.readU16bit(info, pos);
          return memberValuePair(pos + 2, nameIndex);
      }

      int memberValuePair(int pos, int nameIndex) throws Exception {
          return memberValue(pos);
      }

      final int memberValue(int pos) throws Exception {
          int tag = info[pos] & 0xff;
          if (tag == 'e') {
              int typeNameIndex = X_Byte.readU16bit(info, pos + 1);
              int constNameIndex = X_Byte.readU16bit(info, pos + 3);
              enumMemberValue(typeNameIndex, constNameIndex);
              return pos + 5;
          }
          else if (tag == 'c') {
              int index = X_Byte.readU16bit(info, pos + 1);
              classMemberValue(index);
              return pos + 3;
          }
          else if (tag == '@') {
            return annotationMemberValue(pos + 1);
          } else if (tag == '[') {
              int num = X_Byte.readU16bit(info, pos + 1);
              return arrayMemberValue(pos + 3, num);
          }
          else { // primitive types or String.
              int index = X_Byte.readU16bit(info, pos + 1);
              constValueMember(tag, index);
              return pos + 3;
          }
      }

      void constValueMember(int tag, int index) throws Exception {}

      void enumMemberValue(int typeNameIndex, int constNameIndex)
          throws Exception {
      }

      void classMemberValue(int index) throws Exception {}

      int annotationMemberValue(int pos) throws Exception {
          return annotation(pos);
      }

      int arrayMemberValue(int pos, int num) throws Exception {
          for (int i = 0; i < num; ++i) {
              pos = memberValue(pos);
          }

          return pos;
      }
  }

  public static class Copier extends Walker {
      ByteArrayOutputStream output;
      AnnotationsWriter writer;
      ConstPool srcPool, destPool;
      Map<?, ?> classnames;

      /**
       * Constructs a copier.  This copier renames some class names
       * into the new names specified by <code>map</code> when it copies
       * an annotation attribute.
       *
       * @param info      the source attribute.
       * @param src       the constant pool of the source class.
       * @param dest      the constant pool of the destination class.
       * @param map       pairs of replaced and substituted class names.
       *                  It can be null.
       */
      public Copier(byte[] info, ConstPool src, ConstPool dest, Map<?, ?> map) {
          super(info);
          output = new ByteArrayOutputStream();
          writer = new AnnotationsWriter(output, dest);
          srcPool = src;
          destPool = dest;
          classnames = map;
      }

      public byte[] close() throws IOException {
          writer.close();
          return output.toByteArray();
      }

      @Override
      public void parameters(int numParam, int pos) throws Exception {
          writer.numParameters(numParam);
          super.parameters(numParam, pos);
      }

      @Override
      int annotationArray(int pos, int num) throws Exception {
          writer.numAnnotations(num);
          return super.annotationArray(pos, num);
      }

      @Override
      int annotation(int pos, int type, int numPairs) throws Exception {
          writer.annotation(copy(type), numPairs);
          return super.annotation(pos, type, numPairs);
      }

      @Override
      int memberValuePair(int pos, int nameIndex) throws Exception {
          writer.memberValuePair(copy(nameIndex));
          return super.memberValuePair(pos, nameIndex);
      }

      @Override
      void constValueMember(int tag, int index) throws Exception {
          writer.constValueIndex(tag, copy(index));
          super.constValueMember(tag, index);
      }

      @Override
      void enumMemberValue(int typeNameIndex, int constNameIndex)
          throws Exception
      {
          writer.enumConstValue(copy(typeNameIndex), copy(constNameIndex));
          super.enumMemberValue(typeNameIndex, constNameIndex);
      }

      @Override
      void classMemberValue(int index) throws Exception {
          writer.classInfoIndex(copy(index));
          super.classMemberValue(index);
      }

      @Override
      int annotationMemberValue(int pos) throws Exception {
          writer.annotationValue();
          return super.annotationMemberValue(pos);
      }

      @Override
      int arrayMemberValue(int pos, int num) throws Exception {
          writer.arrayValue(num);
          return super.arrayMemberValue(pos, num);
      }

      /**
       * Copies a constant pool entry into the destination constant pool
       * and returns the index of the copied entry.
       *
       * @param srcIndex      the index of the copied entry into the source
       *                      constant pool.
       * @return the index of the copied item into the destination
       *         constant pool.
       */
      int copy(int srcIndex) {
          return srcPool.copy(srcIndex, destPool, classnames);
      }
  }

  public static class Parser extends Walker {
      ConstPool pool;
      Annotation[][] allParams;   // all parameters
      Annotation[] allAnno;       // all annotations
      Annotation currentAnno;     // current annotation
      MemberValue currentMember;  // current member

      /**
       * Constructs a parser.  This parser constructs a parse tree of
       * the annotations.
       *
       * @param info      the attribute.
       * @param src       the constant pool.
       */
      public Parser(byte[] info, ConstPool cp) {
          super(info);
          pool = cp;
      }

      public Annotation[][] parseParameters() throws Exception {
          parameters();
          return allParams;
      }

      public Annotation[] parseAnnotations() throws Exception {
          annotationArray();
          return allAnno;
      }

      public MemberValue parseMemberValue() throws Exception {
          memberValue(0);
          return currentMember;
      }

      @Override
      public void parameters(int numParam, int pos) throws Exception {
          Annotation[][] params = new Annotation[numParam][];
          for (int i = 0; i < numParam; ++i) {
              pos = annotationArray(pos);
              params[i] = allAnno;
          }

          allParams = params;
      }

      @Override
      int annotationArray(int pos, int num) throws Exception {
          Annotation[] array = new Annotation[num];
          for (int i = 0; i < num; ++i) {
              pos = annotation(pos);
              array[i] = currentAnno;
          }

          allAnno = array;
          return pos;
      }

      @Override
      int annotation(int pos, int type, int numPairs) throws Exception {
          currentAnno = new Annotation(type, pool);
          return super.annotation(pos, type, numPairs);
      }

      @Override
      int memberValuePair(int pos, int nameIndex) throws Exception {
          pos = super.memberValuePair(pos, nameIndex);
          currentAnno.addMemberValue(nameIndex, currentMember);
          return pos;
      }

      @Override
      void constValueMember(int tag, int index) throws Exception {
          MemberValue m;
          ConstPool cp = pool;
          switch (tag) {
          case 'B' :
              m = new ByteMemberValue(index, cp);
              break;
          case 'C' :
              m = new CharMemberValue(index, cp);
              break;
          case 'D' :
              m = new DoubleMemberValue(index, cp);
              break;
          case 'F' :
              m = new FloatMemberValue(index, cp);
              break;
          case 'I' :
              m = new IntegerMemberValue(index, cp);
              break;
          case 'J' :
              m = new LongMemberValue(index, cp);
              break;
          case 'S' :
              m = new ShortMemberValue(index, cp);
              break;
          case 'Z' :
              m = new BooleanMemberValue(index, cp);
              break;
          case 's' :
              m = new StringMemberValue(index, cp);
              break;
          default :
              throw new RuntimeException("unknown tag:" + tag);
          }

          currentMember = m;
          super.constValueMember(tag, index);
      }

      @Override
      void enumMemberValue(int typeNameIndex, int constNameIndex)
          throws Exception
      {
          currentMember = new EnumMemberValue(typeNameIndex,
                                            constNameIndex, pool);
          super.enumMemberValue(typeNameIndex, constNameIndex);
      }

      @Override
      void classMemberValue(int index) throws Exception {
          currentMember = new ClassMemberValue(index, pool);
          super.classMemberValue(index);
      }

      @Override
      int annotationMemberValue(int pos) throws Exception {
          Annotation anno = currentAnno;
          pos = super.annotationMemberValue(pos);
          currentMember = new AnnotationMemberValue(currentAnno, pool);
          currentAnno = anno;
          return pos;
      }

      @Override
      int arrayMemberValue(int pos, int num) throws Exception {
          ArrayMemberValue amv = new ArrayMemberValue(pool);
          MemberValue[] elements = new MemberValue[num];
          for (int i = 0; i < num; ++i) {
              pos = memberValue(pos);
              elements[i] = currentMember;
          }

          amv.setValue(elements);
          currentMember = amv;
          return pos;
      }
  }
}

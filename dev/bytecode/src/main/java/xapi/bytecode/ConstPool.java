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
package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ConstPool {
  /**
   * A hash function for CACHE_SIZE
   */
  private static int hashFunc(final int a, final int b) {
    int h = -2128831035;
    final int prime = 16777619;
    h = (h ^ a & 0xff) * prime;
    h = (h ^ b & 0xff) * prime;

    // changing the hash key size from 32bit to 5bit
    h = h >> 5 ^ h & 0x1f;
    return h & 0x1f;    // 0..31
  }
  LongVector items;
  int numOfItems;
  HashMap<String, ClassInfo> classes;
  HashMap<String, ConstInfo> strings;
  ConstInfo[] constInfoCache;
  int[] constInfoIndexCache;

  int thisClassInfo;

  private static final int CACHE_SIZE = 32;

  /**
   * <code>CONSTANT_Class</code>
   */
  public static final int CONST_Class = ClassInfo.tag;

  /**
   * <code>CONSTANT_Fieldref</code>
   */
  public static final int CONST_Fieldref = FieldrefInfo.tag;

  /**
   * <code>CONSTANT_Methodref</code>
   */
  public static final int CONST_Methodref = MethodrefInfo.tag;

  /**
   * <code>CONSTANT_InterfaceMethodref</code>
   */
  public static final int CONST_InterfaceMethodref
  = InterfaceMethodrefInfo.tag;

  /**
   * <code>CONSTANT_String</code>
   */
  public static final int CONST_String = StringInfo.tag;

  /**
   * <code>CONSTANT_Integer</code>
   */
  public static final int CONST_Integer = IntegerInfo.tag;

  /**
   * <code>CONSTANT_Float</code>
   */
  public static final int CONST_Float = FloatInfo.tag;

  /**
   * <code>CONSTANT_Long</code>
   */
  public static final int CONST_Long = LongInfo.tag;

  /**
   * <code>CONSTANT_Double</code>
   */
  public static final int CONST_Double = DoubleInfo.tag;

  /**
   * <code>CONSTANT_NameAndType</code>
   */
  public static final int CONST_NameAndType = NameAndTypeInfo.tag;

  /**
   * <code>CONSTANT_Utf8</code>
   */
  public static final int CONST_Utf8 = Utf8Info.tag;

  /**
   * Represents the class using this constant pool table.
   */
  public static final CtClass THIS = null;

  /**
   * Constructs a constant pool table from the given byte stream.
   *
   * @param in        byte stream.
   */
  public ConstPool(final DataInput in) throws IOException {
    classes = new HashMap<String, ClassInfo>();
    strings = new HashMap<String, ConstInfo>();
    constInfoCache = new ConstInfo[CACHE_SIZE];
    constInfoIndexCache = new int[CACHE_SIZE];
    thisClassInfo = 0;
    /* read() initializes items and numOfItems, and do addItem(null).
     */
    read(in);
  }

  /**
   * Constructs a constant pool table.
   *
   * @param thisclass         the name of the class using this constant
   *                          pool table
   */
  public ConstPool(final String thisclass) {
    items = new LongVector();
    numOfItems = 0;
    addItem(null);          // index 0 is reserved by the JVM.
    classes = new HashMap<String, ClassInfo>();
    strings = new HashMap<String, ConstInfo>();
    constInfoCache = new ConstInfo[CACHE_SIZE];
    constInfoIndexCache = new int[CACHE_SIZE];
    thisClassInfo = addClassInfo(thisclass);
  }

  /**
   * Adds a new <code>CONSTANT_Class_info</code> structure.
   *
   * <p>This also adds a <code>CONSTANT_Utf8_info</code> structure
   * for storing the class name.
   *
   * @return          the index of the added entry.
   */
  public int addClassInfo(final CtClass c) {
    if (c == THIS) {
      return thisClassInfo;
    } else if (!c.isArray()) {
      return addClassInfo(c.getName());
    } else {
      // an array type is recorded in the hashtable with
      // the key "[L<classname>;" instead of "<classname>".
      //
      // note: toJvmName(toJvmName(c)) is equal to toJvmName(c).

      return addClassInfo(Descriptor.toJvmName(c));
    }
  }

  /**
   * Adds a new <code>CONSTANT_Class_info</code> structure.
   *
   * <p>This also adds a <code>CONSTANT_Utf8_info</code> structure
   * for storing the class name.
   *
   * @param qname     a fully-qualified class name
   *                  (or the JVM-internal representation of that name).
   * @return          the index of the added entry.
   */
  public int addClassInfo(final String qname) {
    ClassInfo info = classes.get(qname);
    if (info != null) {
      return info.index;
    } else {
      final int utf8 = addUtf8Info(Descriptor.toJvmName(qname));
      info = new ClassInfo(utf8, numOfItems);
      classes.put(qname, info);
      return addItem(info);
    }
  }

  /**
   * Adds a new <code>CONSTANT_Double_info</code>
   * structure.
   *
   * @return          the index of the added entry.
   */
  public int addDoubleInfo(final double d) {
    final int i = addItem(new DoubleInfo(d));
    addItem(new ConstInfoPadding());
    return i;
  }

  /**
   * Adds a new <code>CONSTANT_Fieldref_info</code> structure.
   *
   * @param classInfo         <code>class_index</code>
   * @param nameAndTypeInfo   <code>name_and_type_index</code>.
   * @return          the index of the added entry.
   */
  public int addFieldrefInfo(final int classInfo, final int nameAndTypeInfo) {
    final int h = hashFunc(classInfo, nameAndTypeInfo);
    final ConstInfo ci = constInfoCache[h];
    if (ci != null && ci instanceof FieldrefInfo && ci.hashCheck(classInfo, nameAndTypeInfo)) {
      return constInfoIndexCache[h];
    } else {
      final FieldrefInfo item = new FieldrefInfo(classInfo, nameAndTypeInfo);
      constInfoCache[h] = item;
      final int i = addItem(item);
      constInfoIndexCache[h] = i;
      return i;
    }
  }

  /**
   * Adds a new <code>CONSTANT_Fieldref_info</code> structure.
   *
   * <p>This also adds a new <code>CONSTANT_NameAndType_info</code>
   * structure.
   *
   * @param classInfo         <code>class_index</code>
   * @param name              <code>name_index</code>
   *                          of <code>CONSTANT_NameAndType_info</code>.
   * @param type              <code>descriptor_index</code>
   *                          of <code>CONSTANT_NameAndType_info</code>.
   * @return          the index of the added entry.
   */
  public int addFieldrefInfo(final int classInfo, final String name, final String type) {
    final int nt = addNameAndTypeInfo(name, type);
    return addFieldrefInfo(classInfo, nt);
  }

  /**
   * Adds a new <code>CONSTANT_Float_info</code>
   * structure.
   *
   * @return          the index of the added entry.
   */
  public int addFloatInfo(final float f) {
    return addItem(new FloatInfo(f));
  }

  /**
   * Adds a new <code>CONSTANT_Integer_info</code>
   * structure.
   *
   * @return          the index of the added entry.
   */
  public int addIntegerInfo(final int i) {
    return addItem(new IntegerInfo(i));
  }

  /**
   * Adds a new <code>CONSTANT_InterfaceMethodref_info</code>
   * structure.
   *
   * @param classInfo         <code>class_index</code>
   * @param nameAndTypeInfo   <code>name_and_type_index</code>.
   * @return          the index of the added entry.
   */
  public int addInterfaceMethodrefInfo(final int classInfo,
      final int nameAndTypeInfo) {
    final int h = hashFunc(classInfo, nameAndTypeInfo);
    final ConstInfo ci = constInfoCache[h];
    if (ci != null && ci instanceof InterfaceMethodrefInfo && ci.hashCheck(classInfo, nameAndTypeInfo)) {
      return constInfoIndexCache[h];
    } else {
      final InterfaceMethodrefInfo item =new InterfaceMethodrefInfo(classInfo, nameAndTypeInfo);
      constInfoCache[h] = item;
      final int i = addItem(item);
      constInfoIndexCache[h] = i;
      return i;
    }
  }

  /**
   * Adds a new <code>CONSTANT_InterfaceMethodref_info</code>
   * structure.
   *
   * <p>This also adds a new <code>CONSTANT_NameAndType_info</code>
   * structure.
   *
   * @param classInfo         <code>class_index</code>
   * @param name              <code>name_index</code>
   *                          of <code>CONSTANT_NameAndType_info</code>.
   * @param type              <code>descriptor_index</code>
   *                          of <code>CONSTANT_NameAndType_info</code>.
   * @return          the index of the added entry.
   */
  public int addInterfaceMethodrefInfo(final int classInfo, final String name,
      final String type) {
    final int nt = addNameAndTypeInfo(name, type);
    return addInterfaceMethodrefInfo(classInfo, nt);
  }

  /**
   * Adds a new <code>CONSTANT_InvokeDynamic_info</code>
   * structure.
   *
   * @param bootstrap     <code>bootstrap_method_attr_index</code>.
   * @param nameAndType   <code>name_and_type_index</code>.
   * @return          the index of the added entry.
   *
   */
  public int addInvokeDynamicInfo(final int bootstrap, final int nameAndType) {
    return addItem(new InvokeDynamicInfo(bootstrap, nameAndType, numOfItems));
  }

  /**
   * Adds a new <code>CONSTANT_Long_info</code>
   * structure.
   *
   * @return          the index of the added entry.
   */
  public int addLongInfo(final long l) {
    final int i = addItem(new LongInfo(l));
    addItem(new ConstInfoPadding());
    return i;
  }

  /**
   * Adds a new <code>CONSTANT_MethodHandle_info</code>
   * structure.
   *
   * @param kind      <code>reference_kind</code>
   *                  such as {@link #REF_invokeStatic <code>REF_invokeStatic</code>}.
   * @param index     <code>reference_index</code>.
   * @return          the index of the added entry.
   *
   * @since 3.17
   */
  public int addMethodHandleInfo(final int kind, final int index) {
    return addItem(new MethodHandleInfo(kind, index, numOfItems));
  }

  /**
   * Adds a new <code>CONSTANT_Methodref_info</code> structure.
   *
   * @param classInfo         <code>class_index</code>
   * @param nameAndTypeInfo   <code>name_and_type_index</code>.
   * @return          the index of the added entry.
   */
  public int addMethodrefInfo(final int classInfo, final int nameAndTypeInfo) {
    final int h = hashFunc(classInfo, nameAndTypeInfo);
    final ConstInfo ci = constInfoCache[h];
    if (ci != null && ci instanceof MethodrefInfo && ci.hashCheck(classInfo, nameAndTypeInfo)) {
      return constInfoIndexCache[h];
    } else {
      final MethodrefInfo item = new MethodrefInfo(classInfo, nameAndTypeInfo);
      constInfoCache[h] = item;
      final int i = addItem(item);
      constInfoIndexCache[h] = i;
      return i;
    }
  }

  /**
   * Adds a new <code>CONSTANT_Methodref_info</code> structure.
   *
   * <p>This also adds a new <code>CONSTANT_NameAndType_info</code>
   * structure.
   *
   * @param classInfo         <code>class_index</code>
   * @param name              <code>name_index</code>
   *                          of <code>CONSTANT_NameAndType_info</code>.
   * @param type              <code>descriptor_index</code>
   *                          of <code>CONSTANT_NameAndType_info</code>.
   * @return          the index of the added entry.
   */
  public int addMethodrefInfo(final int classInfo, final String name, final String type) {
    final int nt = addNameAndTypeInfo(name, type);
    return addMethodrefInfo(classInfo, nt);
  }

  /**
   * Adds a new <code>CONSTANT_MethodType_info</code>
   * structure.
   *
   * @param desc      <code>descriptor_index</code>.
   * @return          the index of the added entry.
   *
   */
  public int addMethodTypeInfo(final int desc) {
    return addItem(new MethodTypeInfo(desc, numOfItems));
  }

  /**
   * Adds a new <code>CONSTANT_NameAndType_info</code> structure.
   *
   * @param name      <code>name_index</code>
   * @param type      <code>descriptor_index</code>
   * @return          the index of the added entry.
   */
  public int addNameAndTypeInfo(final int name, final int type) {
    final int h = hashFunc(name, type);
    final ConstInfo ci = constInfoCache[h];
    if (ci != null && ci instanceof NameAndTypeInfo && ci.hashCheck(name, type)) {
      return constInfoIndexCache[h];
    } else {
      final NameAndTypeInfo item = new NameAndTypeInfo(name, type);
      constInfoCache[h] = item;
      final int i = addItem(item);
      constInfoIndexCache[h] = i;
      return i;
    }
  }

  /**
   * Adds a new <code>CONSTANT_NameAndType_info</code> structure.
   *
   * <p>This also adds <code>CONSTANT_Utf8_info</code> structures.
   *
   * @param name      <code>name_index</code>
   * @param type      <code>descriptor_index</code>
   * @return          the index of the added entry.
   */
  public int addNameAndTypeInfo(final String name, final String type) {
    return addNameAndTypeInfo(addUtf8Info(name), addUtf8Info(type));
  }

  /**
   * Adds a new <code>CONSTANT_String_info</code>
   * structure.
   *
   * <p>This also adds a new <code>CONSTANT_Utf8_info</code>
   * structure.
   *
   * @return          the index of the added entry.
   */
  public int addStringInfo(final String str) {
    return addItem(new StringInfo(addUtf8Info(str)));
  }

  /**
   * Adds a new <code>CONSTANT_Utf8_info</code>
   * structure.
   *
   * <p>If the given utf8 string has been already recorded in the
   * table, then this method does not add a new entry to avoid adding
   * a duplicated entry.
   * Instead, it returns the index of the entry already recorded.
   *
   * @return          the index of the added entry.
   */
  public int addUtf8Info(final String utf8) {
    Utf8Info info = (Utf8Info)strings.get(utf8);
    if (info != null) {
      return info.index;
    } else {
      info = new Utf8Info(utf8, numOfItems);
      strings.put(utf8, info);
      return addItem(info);
    }
  }

  /**
   * Copies the n-th item in this ConstPool object into the destination
   * ConstPool object.
   * The class names that the item refers to are renamed according
   * to the given map.
   *
   * @param n                 the <i>n</i>-th item
   * @param dest              destination constant pool table
   * @param classnames        the map or null.
   * @return the index of the copied item into the destination ClassPool.
   */
  public int copy(final int n, final ConstPool dest, final Map<?, ?> classnames) {
    if (n == 0) {
      return 0;
    }

    final ConstInfo info = getItem(n);
    return info.copy(this, dest, classnames);
  }

  /**
   * Determines whether <code>CONSTANT_Methodref_info</code>,
   * <code>CONSTANT_Fieldref_info</code>, or
   * <code>CONSTANT_InterfaceMethodref_info</code> structure
   * at the given index has the name and the descriptor
   * given as the arguments.
   *
   * @param membername        the member name
   * @param desc              the descriptor of the member.
   * @param index             the index into the constant pool table
   *
   * @return          the name of the target class specified by
   *                  the <code>..._info</code> structure
   *                  at <code>index</code>.
   *                  Otherwise, null if that structure does not
   *                  match the given member name and descriptor.
   */
  public String eqMember(final String membername, final String desc, final int index) {
    final MemberrefInfo minfo = (MemberrefInfo)getItem(index);
    final NameAndTypeInfo ntinfo
    = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
    if (getUtf8Info(ntinfo.memberName).equals(membername)
        && getUtf8Info(ntinfo.typeDescriptor).equals(desc)) {
      return getClassInfo(minfo.classIndex);
    }
    else {
      return null;       // false
    }
  }

  /**
   * Reads <code>CONSTANT_Class_info</code> structure
   * at the given index.
   *
   * @return  a fully-qualified class or interface name specified
   *          by <code>name_index</code>.  If the type is an array
   *          type, this method returns an encoded name like
   *          <code>[java.lang.Object;</code> (note that the separators
   *          are not slashes but dots).
   * @see javassist.ClassPool#getCtClass(String)
   */
  public String getClassInfo(final int index) {
    final ClassInfo c = (ClassInfo)getItem(index);
    if (c == null) {
      return null;
    } else {
      return Descriptor.toJavaName(getUtf8Info(c.name));
    }
  }

  /**
   * Returns the name of the class using this constant pool table.
   */
  public String getClassName() {
    return getClassInfo(thisClassInfo);
  }

  /**
   * Get all the class names.
   *
   * @return a set of class names
   */
  public Set<String> getClassNames()
  {
    final HashSet<String> result = new HashSet<String>();
    final LongVector v = items;
    final int size = numOfItems;
    for (int i = 1; i < size; ++i) {
      final String className = v.elementAt(i).getClassName(this);
      if (className != null) {
        result.add(className);
      }
    }
    return result;
  }

  /**
   * Reads <code>CONSTANT_Double_info</code> structure
   * at the given index.
   *
   * @return the value specified by this entry.
   */
  public double getDoubleInfo(final int index) {
    final DoubleInfo i = (DoubleInfo)getItem(index);
    return i.value;
  }

  /**
   * Reads the <code>class_index</code> field of the
   * <code>CONSTANT_Fieldref_info</code> structure
   * at the given index.
   */
  public int getFieldrefClass(final int index) {
    final FieldrefInfo finfo = (FieldrefInfo)getItem(index);
    return finfo.classIndex;
  }

  /**
   * Reads the <code>class_index</code> field of the
   * <code>CONSTANT_Fieldref_info</code> structure
   * at the given index.
   *
   * @return the name of the class at that <code>class_index</code>.
   */
  public String getFieldrefClassName(final int index) {
    final FieldrefInfo f = (FieldrefInfo)getItem(index);
    if (f == null) {
      return null;
    } else {
      return getClassInfo(f.classIndex);
    }
  }
  /**
   * Reads the <code>name_index</code> field of the
   * <code>CONSTANT_NameAndType_info</code> structure
   * indirectly specified by the given index.
   *
   * @param index     an index to a <code>CONSTANT_Fieldref_info</code>.
   * @return  the name of the field.
   */
  public String getFieldrefName(final int index) {
    final FieldrefInfo f = (FieldrefInfo)getItem(index);
    if (f == null) {
      return null;
    } else {
      final NameAndTypeInfo n = (NameAndTypeInfo)getItem(f.nameAndTypeIndex);
      if(n == null) {
        return null;
      } else {
        return getUtf8Info(n.memberName);
      }
    }
  }

  /**
   * Reads the <code>name_and_type_index</code> field of the
   * <code>CONSTANT_Fieldref_info</code> structure
   * at the given index.
   */
  public int getFieldrefNameAndType(final int index) {
    final FieldrefInfo finfo = (FieldrefInfo)getItem(index);
    return finfo.nameAndTypeIndex;
  }

  /**
   * Reads the <code>descriptor_index</code> field of the
   * <code>CONSTANT_NameAndType_info</code> structure
   * indirectly specified by the given index.
   *
   * @param index     an index to a <code>CONSTANT_Fieldref_info</code>.
   * @return  the type descriptor of the field.
   */
  public String getFieldrefType(final int index) {
    final FieldrefInfo f = (FieldrefInfo)getItem(index);
    if (f == null) {
      return null;
    } else {
      final NameAndTypeInfo n = (NameAndTypeInfo)getItem(f.nameAndTypeIndex);
      if(n == null) {
        return null;
      } else {
        return getUtf8Info(n.typeDescriptor);
      }
    }
  }

  /**
   * Reads <code>CONSTANT_Float_info</code> structure
   * at the given index.
   *
   * @return the value specified by this entry.
   */
  public float getFloatInfo(final int index) {
    final FloatInfo i = (FloatInfo)getItem(index);
    return i.value;
  }

  /**
   * Reads <code>CONSTANT_Integer_info</code> structure
   * at the given index.
   *
   * @return the value specified by this entry.
   */
  public int getIntegerInfo(final int index) {
    final IntegerInfo i = (IntegerInfo)getItem(index);
    return i.value;
  }

  /**
   * Reads the <code>class_index</code> field of the
   * <code>CONSTANT_InterfaceMethodref_info</code> structure
   * at the given index.
   */
  public int getInterfaceMethodrefClass(final int index) {
    final InterfaceMethodrefInfo minfo
    = (InterfaceMethodrefInfo)getItem(index);
    return minfo.classIndex;
  }

  /**
   * Reads the <code>class_index</code> field of the
   * <code>CONSTANT_InterfaceMethodref_info</code> structure
   * at the given index.
   *
   * @return the name of the class at that <code>class_index</code>.
   */
  public String getInterfaceMethodrefClassName(final int index) {
    final InterfaceMethodrefInfo minfo
    = (InterfaceMethodrefInfo)getItem(index);
    return getClassInfo(minfo.classIndex);
  }

  /**
   * Reads the <code>name_index</code> field of the
   * <code>CONSTANT_NameAndType_info</code> structure
   * indirectly specified by the given index.
   *
   * @param index     an index to
   *                  a <code>CONSTANT_InterfaceMethodref_info</code>.
   * @return  the name of the method.
   */
  public String getInterfaceMethodrefName(final int index) {
    final InterfaceMethodrefInfo minfo
    = (InterfaceMethodrefInfo)getItem(index);
    if (minfo == null) {
      return null;
    } else {
      final NameAndTypeInfo n
      = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
      if(n == null) {
        return null;
      } else {
        return getUtf8Info(n.memberName);
      }
    }
  }

  /**
   * Reads the <code>name_and_type_index</code> field of the
   * <code>CONSTANT_InterfaceMethodref_info</code> structure
   * at the given index.
   */
  public int getInterfaceMethodrefNameAndType(final int index) {
    final InterfaceMethodrefInfo minfo
    = (InterfaceMethodrefInfo)getItem(index);
    return minfo.nameAndTypeIndex;
  }

  /**
   * Reads the <code>descriptor_index</code> field of the
   * <code>CONSTANT_NameAndType_info</code> structure
   * indirectly specified by the given index.
   *
   * @param index     an index to
   *                  a <code>CONSTANT_InterfaceMethodref_info</code>.
   * @return  the descriptor of the method.
   */
  public String getInterfaceMethodrefType(final int index) {
    final InterfaceMethodrefInfo minfo
    = (InterfaceMethodrefInfo)getItem(index);
    if (minfo == null) {
      return null;
    } else {
      final NameAndTypeInfo n
      = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
      if(n == null) {
        return null;
      } else {
        return getUtf8Info(n.typeDescriptor);
      }
    }
  }

  public ConstInfo getItem(final int n) {
    return items.elementAt(n);
  }

  /**
   * Reads <code>CONSTANT_Integer_info</code>, <code>_Float_info</code>,
   * <code>_Long_info</code>, <code>_Double_info</code>, or
   * <code>_String_info</code> structure.
   * These are used with the LDC instruction.
   *
   * @return a <code>String</code> value or a wrapped primitive-type
   * value.
   */
  public Object getLdcValue(final int index) {
    final ConstInfo constInfo = this.getItem(index);
    Object value = null;
    if (constInfo instanceof StringInfo) {
      value = this.getStringInfo(index);
    } else if (constInfo instanceof FloatInfo) {
      value = new Float(getFloatInfo(index));
    } else if (constInfo instanceof IntegerInfo) {
      value = new Integer(getIntegerInfo(index));
    } else if (constInfo instanceof LongInfo) {
      value = new Long(getLongInfo(index));
    } else if (constInfo instanceof DoubleInfo) {
      value = new Double(getDoubleInfo(index));
    } else {
      value = null;
    }

    return value;
  }

  /**
   * Reads <code>CONSTANT_Long_info</code> structure
   * at the given index.
   *
   * @return the value specified by this entry.
   */
  public long getLongInfo(final int index) {
    final LongInfo i = (LongInfo)getItem(index);
    return i.value;
  }

  /**
   * Reads the <code>class_index</code> field of the
   * <code>CONSTANT_Fieldref_info</code>,
   * <code>CONSTANT_Methodref_info</code>,
   * or <code>CONSTANT_Interfaceref_info</code>,
   * structure at the given index.
   *
   * @since 3.6
   */
  public int getMemberClass(final int index) {
    final MemberrefInfo minfo = (MemberrefInfo)getItem(index);
    return minfo.classIndex;
  }

  /**
   * Reads the <code>name_and_type_index</code> field of the
   * <code>CONSTANT_Fieldref_info</code>,
   * <code>CONSTANT_Methodref_info</code>,
   * or <code>CONSTANT_Interfaceref_info</code>,
   * structure at the given index.
   *
   * @since 3.6
   */
  public int getMemberNameAndType(final int index) {
    final MemberrefInfo minfo = (MemberrefInfo)getItem(index);
    return minfo.nameAndTypeIndex;
  }

  /**
   * Reads the <code>class_index</code> field of the
   * <code>CONSTANT_Methodref_info</code> structure
   * at the given index.
   */
  public int getMethodrefClass(final int index) {
    final MethodrefInfo minfo = (MethodrefInfo)getItem(index);
    return minfo.classIndex;
  }

  /**
   * Reads the <code>class_index</code> field of the
   * <code>CONSTANT_Methodref_info</code> structure
   * at the given index.
   *
   * @return the name of the class at that <code>class_index</code>.
   */
  public String getMethodrefClassName(final int index) {
    final MethodrefInfo minfo = (MethodrefInfo)getItem(index);
    if (minfo == null) {
      return null;
    } else {
      return getClassInfo(minfo.classIndex);
    }
  }

  /**
   * Reads the <code>name_index</code> field of the
   * <code>CONSTANT_NameAndType_info</code> structure
   * indirectly specified by the given index.
   *
   * @param index     an index to a <code>CONSTANT_Methodref_info</code>.
   * @return  the name of the method.
   */
  public String getMethodrefName(final int index) {
    final MethodrefInfo minfo = (MethodrefInfo)getItem(index);
    if (minfo == null) {
      return null;
    } else {
      final NameAndTypeInfo n
      = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
      if(n == null) {
        return null;
      } else {
        return getUtf8Info(n.memberName);
      }
    }
  }

  /**
   * Reads the <code>name_and_type_index</code> field of the
   * <code>CONSTANT_Methodref_info</code> structure
   * at the given index.
   */
  public int getMethodrefNameAndType(final int index) {
    final MethodrefInfo minfo = (MethodrefInfo)getItem(index);
    return minfo.nameAndTypeIndex;
  }

  /**
   * Reads the <code>descriptor_index</code> field of the
   * <code>CONSTANT_NameAndType_info</code> structure
   * indirectly specified by the given index.
   *
   * @param index     an index to a <code>CONSTANT_Methodref_info</code>.
   * @return  the descriptor of the method.
   */
  public String getMethodrefType(final int index) {
    final MethodrefInfo minfo = (MethodrefInfo)getItem(index);
    if (minfo == null) {
      return null;
    } else {
      final NameAndTypeInfo n
      = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
      if(n == null) {
        return null;
      } else {
        return getUtf8Info(n.typeDescriptor);
      }
    }
  }

  /**
   * Reads the <code>descriptor_index</code> field of the
   * <code>CONSTANT_NameAndType_info</code> structure
   * at the given index.
   */
  public int getNameAndTypeDescriptor(final int index) {
    final NameAndTypeInfo ntinfo = (NameAndTypeInfo)getItem(index);
    return ntinfo.typeDescriptor;
  }

  /**
   * Reads the <code>name_index</code> field of the
   * <code>CONSTANT_NameAndType_info</code> structure
   * at the given index.
   */
  public int getNameAndTypeName(final int index) {
    final NameAndTypeInfo ntinfo = (NameAndTypeInfo)getItem(index);
    return ntinfo.memberName;
  }

  /**
   * Returns the number of entries in this table.
   */
  public int getSize() {
    return numOfItems;
  }

  /**
   * Reads <code>CONSTANT_String_info</code> structure
   * at the given index.
   *
   * @return the string specified by <code>string_index</code>.
   */
  public String getStringInfo(final int index) {
    final StringInfo si = (StringInfo)getItem(index);
    return getUtf8Info(si.string);
  }

  /**
   * Returns the <code>tag</code> field of the constant pool table
   * entry at the given index.
   */
  public int getTag(final int index) {
    return getItem(index).getTag();
  }

  /**
   * Returns the index of <code>CONSTANT_Class_info</code> structure
   * specifying the class using this constant pool table.
   */
  public int getThisClassInfo() {
    return thisClassInfo;
  }

  /**
   * Reads <code>CONSTANT_utf8_info</code> structure
   * at the given index.
   *
   * @return the string specified by this entry.
   */
  public String getUtf8Info(final int index) {
    final Utf8Info utf = (Utf8Info)getItem(index);
    return utf.string;
  }

  /**
   * Determines whether <code>CONSTANT_Methodref_info</code>
   * structure at the given index represents the constructor
   * of the given class.
   *
   * @return          the <code>descriptor_index</code> specifying
   *                  the type descriptor of the that constructor.
   *                  If it is not that constructor,
   *                  <code>isConstructor()</code> returns 0.
   */
  public int isConstructor(final String classname, final int index) {
    return isMember(classname, MethodInfo.nameInit, index);
  }

  /**
   * Determines whether <code>CONSTANT_Methodref_info</code>,
   * <code>CONSTANT_Fieldref_info</code>, or
   * <code>CONSTANT_InterfaceMethodref_info</code> structure
   * at the given index represents the member with the specified
   * name and declaring class.
   *
   * @param classname         the class declaring the member
   * @param membername        the member name
   * @param index             the index into the constant pool table
   *
   * @return          the <code>descriptor_index</code> specifying
   *                  the type descriptor of that member.
   *                  If it is not that member,
   *                  <code>isMember()</code> returns 0.
   */
  public int isMember(final String classname, final String membername, final int index) {
    final MemberrefInfo minfo = (MemberrefInfo)getItem(index);
    if (getClassInfo(minfo.classIndex).equals(classname)) {
      final NameAndTypeInfo ntinfo
      = (NameAndTypeInfo)getItem(minfo.nameAndTypeIndex);
      if (getUtf8Info(ntinfo.memberName).equals(membername)) {
        return ntinfo.typeDescriptor;
      }
    }

    return 0;       // false
  }

  /**
   * Prints the contents of the constant pool table.
   */
  public void print() {
    print(new PrintWriter(System.out, true));
  }

  /**
   * Prints the contents of the constant pool table.
   */
  public void print(final PrintWriter out) {
    final int size = numOfItems;
    for (int i = 1; i < size; ++i) {
      out.print(i);
      out.print(" ");
      items.elementAt(i).print(out);
    }
  }

  /**
   * Replaces all occurrences of class names.
   *
   * @param classnames        specifies pairs of replaced and substituted
   *                          name.
   */
  public void renameClass(final Map<?, ?> classnames) {
    final LongVector v = items;
    final int size = numOfItems;
    classes = new HashMap<String, ClassInfo>(classes.size() * 2);
    for (int i = 1; i < size; ++i) {
      final ConstInfo ci = v.elementAt(i);
      ci.renameClass(this, classnames);
      ci.makeHashtable(this);
    }
  }

  /**
   * Replaces all occurrences of a class name.
   *
   * @param oldName           the replaced name (JVM-internal representation).
   * @param newName           the substituted name (JVM-internal representation).
   */
  public void renameClass(final String oldName, final String newName) {
    final LongVector v = items;
    final int size = numOfItems;
    classes = new HashMap<String, ClassInfo>(classes.size() * 2);
    for (int i = 1; i < size; ++i) {
      final ConstInfo ci = v.elementAt(i);
      ci.renameClass(this, oldName, newName);
      ci.makeHashtable(this);
    }
  }

  /**
   * Writes the contents of the constant pool table.
   */
  public void write(final DataOutput out) throws IOException {
    out.writeShort(numOfItems);
    final LongVector v = items;
    final int size = numOfItems;
    for (int i = 1; i < size; ++i) {
      v.elementAt(i).write(out);
    }
  }

  int addConstInfoPadding() {
    return addItem(new ConstInfoPadding());
  }

  void prune() {
    classes = new HashMap<String, ClassInfo>();
    strings = new HashMap<String, ConstInfo>();
    constInfoCache = new ConstInfo[CACHE_SIZE];
    constInfoIndexCache = new int[CACHE_SIZE];
  }

  void setThisClassInfo(final int i) {
    thisClassInfo = i;
  }
  private int addItem(final ConstInfo info) {
    items.addElement(info);
    return numOfItems++;
  }

  private void read(final DataInput in) throws IOException {
    int n = in.readUnsignedShort();

    items = new LongVector(n);
    numOfItems = 0;
    addItem(null);          // index 0 is reserved by the JVM.

    while (--n > 0) {       // index 0 is reserved by JVM
      final int tag = readOne(in);
      if (tag == LongInfo.tag || tag == DoubleInfo.tag) {
        addItem(new ConstInfoPadding());
        --n;
      }
    }

    int i = 1;
    while (true) {
      final ConstInfo info = items.elementAt(i++);
      if (info == null) {
        break;
      } else {
        info.makeHashtable(this);
      }
    }
  }

  private int readOne(final DataInput in) throws IOException {
    ConstInfo info;
    final int tag = in.readUnsignedByte();
    switch (tag) {
    case Utf8Info.tag :                     // 1
      info = new Utf8Info(in, numOfItems);
      strings.put(((Utf8Info)info).string, info);
      break;
    case IntegerInfo.tag :                  // 3
      info = new IntegerInfo(in);
      break;
    case FloatInfo.tag :                    // 4
      info = new FloatInfo(in);
      break;
    case LongInfo.tag :                     // 5
      info = new LongInfo(in);
      break;
    case DoubleInfo.tag :                   // 6
      info = new DoubleInfo(in);
      break;
    case ClassInfo.tag :                    // 7
      info = new ClassInfo(in, numOfItems);
      // classes.put(<classname>, info);
      break;
    case StringInfo.tag :                   // 8
      info = new StringInfo(in);
      break;
    case FieldrefInfo.tag :                 // 9
      info = new FieldrefInfo(in);
      break;
    case MethodrefInfo.tag :                // 10
      info = new MethodrefInfo(in);
      break;
    case InterfaceMethodrefInfo.tag :       // 11
      info = new InterfaceMethodrefInfo(in);
      break;
    case NameAndTypeInfo.tag :              // 12
      info = new NameAndTypeInfo(in);
      break;
    case MethodHandleInfo.tag :             // 15
      info = new MethodHandleInfo(in, numOfItems);
      break;
    case MethodTypeInfo.tag :               // 16
      info = new MethodTypeInfo(in, numOfItems);
      break;
    case InvokeDynamicInfo.tag :            // 18
      info = new InvokeDynamicInfo(in, numOfItems);
      break;
    default :
      throw new IOException("invalid constant type: " + tag);
    }

    addItem(info);
    return tag;
  }
}

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
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import xapi.bytecode.annotation.Annotation;
import xapi.bytecode.annotation.AnnotationsAttribute;
import xapi.bytecode.attributes.AttributeInfo;
import xapi.bytecode.attributes.InnerClassesAttribute;
import xapi.bytecode.attributes.SignatureAttribute;
import xapi.bytecode.attributes.SourceFileAttribute;
import xapi.bytecode.impl.BytecodeUtil;
import xapi.source.X_Modifier;
import xapi.source.X_Source;
import xapi.util.X_Util;


public final class ClassFile implements Annotated {
    int major, minor; // version number
    ConstPool constPool;
    int thisClass;
    int accessFlags;
    int superClass;
    int[] interfaces;
    ArrayList<FieldInfo> fields;
    ArrayList<MethodInfo> methods;
    ArrayList<AttributeInfo> attributes;
    String thisclassname; // not JVM-internal name
    String[] cachedInterfaces;
    String cachedSuperclass;

    /**
     * The major version number of class files
     * for JDK 1.1.
     */
    public static final int JAVA_1 = 45;

    /**
     * The major version number of class files
     * for JDK 1.2.
     */
    public static final int JAVA_2 = 46;

    /**
     * The major version number of class files
     * for JDK 1.3.
     */
    public static final int JAVA_3 = 47;

    /**
     * The major version number of class files
     * for JDK 1.4.
     */
    public static final int JAVA_4 = 48;

    /**
     * The major version number of class files
     * for JDK 1.5.
     */
    public static final int JAVA_5 = 49;

    /**
     * The major version number of class files
     * for JDK 1.6.
     */
    public static final int JAVA_6 = 50;

    /**
     * The major version number of class files
     * for JDK 1.7.
     */
    public static final int JAVA_7 = 51;

    /**
     * The major version number of class files created
     * from scratch.  The default value is 47 (JDK 1.3)
     * or 49 (JDK 1.5) if the JVM supports <code>java.lang.StringBuilder</code>.
     */
    public static int MAJOR_VERSION = JAVA_3;

    static {
        try {
            Class.forName("java.lang.StringBuilder");
            MAJOR_VERSION = JAVA_5;
        }
        catch (Throwable t) {}
    }

    /**
     * Constructs a class file from a byte stream.
     */
    public ClassFile(DataInput in) throws IOException {
        read(in);
    }

    /**
     * Constructs a class file including no members.
     *
     * @param isInterface
     *            true if this is an interface. false if this is a class.
     * @param classname
     *            a fully-qualified class name
     * @param superclass
     *            a fully-qualified super class name
     */
    public ClassFile(boolean isInterface, String classname, String superclass) {
        major = MAJOR_VERSION;
        minor = 0; // JDK 1.3 or later
        constPool = new ConstPool(classname);
        thisClass = constPool.getThisClassInfo();
        if (isInterface) {
          accessFlags = X_Modifier.INTERFACE | X_Modifier.ABSTRACT;
        } else {
          accessFlags = X_Modifier.SUPER;
        }

        initSuperclass(superclass);
        interfaces = null;
        fields = new ArrayList<FieldInfo>();
        methods = new ArrayList<MethodInfo>();
        thisclassname = classname;

        attributes = new ArrayList<AttributeInfo>();
        attributes.add(new SourceFileAttribute(constPool,
                getSourcefileName(thisclassname)));
    }

    private void initSuperclass(String superclass) {
        if (superclass != null) {
            this.superClass = constPool.addClassInfo(superclass);
            cachedSuperclass = superclass;
        }
        else {
            this.superClass = constPool.addClassInfo("java.lang.Object");
            cachedSuperclass = "java.lang.Object";
        }
    }

    private static String getSourcefileName(String qname) {
        int index = qname.lastIndexOf('.');
        if (index >= 0) {
          qname = qname.substring(index + 1);
        }

        return qname + ".java";
    }

    /**
     * Eliminates dead constant pool items. If a method or a field is removed,
     * the constant pool items used by that method/field become dead items. This
     * method recreates a constant pool.
     */
    public void compact() {
        ConstPool cp = compact0();
        int n = methods.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = methods.get(i);
            minfo.compact(cp);
        }

        n = fields.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = fields.get(i);
            finfo.compact(cp);
        }

        attributes = AttributeInfo.copyAll(attributes, cp);
        constPool = cp;
    }

    private ConstPool compact0() {
        ConstPool cp = new ConstPool(thisclassname);
        thisClass = cp.getThisClassInfo();
        String sc = getSuperclass();
        if (sc != null) {
          superClass = cp.addClassInfo(getSuperclass());
        }

        if (interfaces != null) {
            int n = interfaces.length;
            for (int i = 0; i < n; ++i) {
              interfaces[i]
                  = cp.addClassInfo(constPool.getClassInfo(interfaces[i]));
            }
        }

        return cp;
    }

    /**
     * Discards all attributes, associated with both the class file and the
     * members such as a code attribute and exceptions attribute. The unused
     * constant pool entries are also discarded (a new packed constant pool is
     * constructed).
     */
    public void prune() {
        ConstPool cp = compact0();
        ArrayList<AttributeInfo> newAttributes = new ArrayList<AttributeInfo>();
        AttributeInfo invisibleAnnotations
            = getAttribute(AnnotationsAttribute.invisibleTag);
        if (invisibleAnnotations != null) {
            invisibleAnnotations = invisibleAnnotations.copy(cp, null);
            newAttributes.add(invisibleAnnotations);
        }

        AttributeInfo visibleAnnotations
            = getAttribute(AnnotationsAttribute.visibleTag);
        if (visibleAnnotations != null) {
            visibleAnnotations = visibleAnnotations.copy(cp, null);
            newAttributes.add(visibleAnnotations);
        }

        AttributeInfo signature
            = getAttribute(SignatureAttribute.tag);
        if (signature != null) {
            signature = signature.copy(cp, null);
            newAttributes.add(signature);
        }

        int n = methods.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = methods.get(i);
            minfo.prune(cp);
        }

        n = fields.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = fields.get(i);
            finfo.prune(cp);
        }

        attributes = newAttributes;
        constPool = cp;
    }

    /**
     * Returns a constant pool table.
     */
    public ConstPool getConstPool() {
        return constPool;
    }

    /**
     * Returns true if this is an interface.
     */
    public boolean isInterface() {
        return (accessFlags & X_Modifier.INTERFACE) != 0;
    }

    /**
     * Returns true if this is a final class or interface.
     */
    public boolean isFinal() {
        return (accessFlags & X_Modifier.FINAL) != 0;
    }

    /**
     * Returns true if this is an abstract class or an interface.
     */
    public boolean isAbstract() {
        return (accessFlags & X_Modifier.ABSTRACT) != 0;
    }

    /**
     * Returns access flags.
     *
     * @see javassist.bytecode.AccessFlag
     */
    public int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Changes access flags.
     *
     * @see javassist.bytecode.AccessFlag
     */
    public void setAccessFlags(int acc) {
        if ((acc & X_Modifier.INTERFACE) == 0) {
          acc |= X_Modifier.SUPER;
        }

        accessFlags = acc;
    }

    /**
     * Returns access and property flags of this nested class.
     * This method returns -1 if the class is not a nested class.
     *
     * <p>The returned value is obtained from <code>inner_class_access_flags</code>
     * of the entry representing this nested class itself
     * in <code>InnerClasses_attribute</code>>.
     */
    public int getInnerAccessFlags() {
        InnerClassesAttribute ica
            = (InnerClassesAttribute)getAttribute(InnerClassesAttribute.tag);
        if (ica == null) {
          return -1;
        }

        String name = getName();
        int n = ica.tableLength();
        for (int i = 0; i < n; ++i) {
          if (name.equals(ica.innerClass(i))) {
            return ica.accessFlags(i);
          }
        }

        return -1;
    }

    public int getThisClassIndex() {
      return thisClass;
    }
    /**
     * Returns the class name.
     */
    public String getName() {
        return thisclassname;
    }

    /**
     * Sets the class name. This method substitutes the new name for all
     * occurrences of the old class name in the class file.
     */
    public void setName(String name) {
        renameClass(thisclassname, name);
    }

    /**
     * Returns the super class name.
     */
    public String getSuperclass() {
        if (cachedSuperclass == null) {
          cachedSuperclass = constPool.getClassInfo(superClass);
        }

        return cachedSuperclass;
    }

    /**
     * Returns the index of the constant pool entry representing the super
     * class.
     */
    public int getSuperclassId() {
        return superClass;
    }

    /**
     * Sets the super class.
     *
     * <p>
     * The new super class should inherit from the old super class.
     * This method modifies constructors so that they call constructors declared
     * in the new super class.
     */
    public void setSuperclass(String superclass) {
        if (superclass == null) {
          superclass = "java.lang.Object";
        }

//        try {
            this.superClass = constPool.addClassInfo(superclass);
//            ArrayList list = methods;
//            int n = list.size();
//            for (int i = 0; i < n; ++i) {
//                MethodInfo minfo = (MethodInfo)list.get(i);
//                minfo.setSuperclass(superclass);
//            }
//        }
//        catch (BadBytecode e) {
//            throw new CannotCompileException(e);
//        }
        cachedSuperclass = superclass;
    }

    /**
     * Replaces all occurrences of a class name in the class file.
     *
     * <p>
     * If class X is substituted for class Y in the class file, X and Y must
     * have the same signature. If Y provides a method m(), X must provide it
     * even if X inherits m() from the super class. If this fact is not
     * guaranteed, the bytecode verifier may cause an error.
     *
     * @param oldname
     *            the replaced class name
     * @param newname
     *            the substituted class name
     */
    public final void renameClass(String oldname, String newname) {
        int n;

        if (oldname.equals(newname)) {
          return;
        }

        if (oldname.equals(thisclassname)) {
          thisclassname = newname;
        }

        oldname = Descriptor.toJvmName(oldname);
        newname = Descriptor.toJvmName(newname);
        constPool.renameClass(oldname, newname);

        AttributeInfo.renameClass(attributes, oldname, newname);
        n = methods.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)methods.get(i);
            String desc = minfo.getDescriptor();
            minfo.setDescriptor(Descriptor.rename(desc, oldname, newname));
            AttributeInfo.renameClass(minfo.getAttributes(), oldname, newname);
        }

        n = fields.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = fields.get(i);
            String desc = finfo.getDescriptor();
            finfo.setDescriptor(Descriptor.rename(desc, oldname, newname));
            AttributeInfo.renameClass(finfo.getAttributes(), oldname, newname);
        }
    }

    /**
     * Replaces all occurrences of several class names in the class file.
     *
     * @param classnames
     *            specifies which class name is replaced with which new name.
     *            Class names must be described with the JVM-internal
     *            representation like <code>java/lang/Object</code>.
     * @see #renameClass(String,String)
     */
    public final void renameClass(Map<?, ?> classnames) {
        String jvmNewThisName = (String)classnames.get(Descriptor
                .toJvmName(thisclassname));
        if (jvmNewThisName != null) {
          thisclassname = Descriptor.toJavaName(jvmNewThisName);
        }

        constPool.renameClass(classnames);

        AttributeInfo.renameClass(attributes, classnames);
        int n = methods.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)methods.get(i);
            String desc = minfo.getDescriptor();
            minfo.setDescriptor(Descriptor.rename(desc, classnames));
            AttributeInfo.renameClass(minfo.getAttributes(), classnames);
        }

        n = fields.size();
        for (int i = 0; i < n; ++i) {
            FieldInfo finfo = (FieldInfo)fields.get(i);
            String desc = finfo.getDescriptor();
            finfo.setDescriptor(Descriptor.rename(desc, classnames));
            AttributeInfo.renameClass(finfo.getAttributes(), classnames);
        }
    }

    /**
     * Returns the names of the interfaces implemented by the class.
     * The returned array is read only.
     */
    public String[] getInterfaces() {
        if (cachedInterfaces != null) {
          return cachedInterfaces;
        }

        String[] rtn = null;
        if (interfaces == null) {
          rtn = new String[0];
        } else {
            int n = interfaces.length;
            String[] list = new String[n];
            for (int i = 0; i < n; ++i) {
              list[i] = constPool.getClassInfo(interfaces[i]);
            }

            rtn = list;
        }

        cachedInterfaces = rtn;
        return rtn;
    }

    /**
     * Sets the interfaces.
     *
     * @param nameList
     *            the names of the interfaces.
     */
    public void setInterfaces(String[] nameList) {
        cachedInterfaces = null;
        if (nameList != null) {
            int n = nameList.length;
            interfaces = new int[n];
            for (int i = 0; i < n; ++i) {
              interfaces[i] = constPool.addClassInfo(nameList[i]);
            }
        }
    }

    /**
     * Appends an interface to the interfaces implemented by the class.
     */
    public void addInterface(String name) {
        cachedInterfaces = null;
        int info = constPool.addClassInfo(name);
        if (interfaces == null) {
            interfaces = new int[1];
            interfaces[0] = info;
        }
        else {
            int n = interfaces.length;
            int[] newarray = new int[n + 1];
            System.arraycopy(interfaces, 0, newarray, 0, n);
            newarray[n] = info;
            interfaces = newarray;
        }
    }

    /**
     * Returns all the fields declared in the class.
     *
     * @return a list of <code>FieldInfo</code>.
     * @see FieldInfo
     */
    public List<FieldInfo> getFields() {
        return fields;
    }

    /**
     * Appends a field to the class.
     * Does not check for existing fields.
     */
    public void addField(FieldInfo finfo) {
        fields.add(finfo);
    }

    /**
     * Returns all the methods declared in the class.
     *
     * @return a list of <code>MethodInfo</code>.
     * @see MethodInfo
     */
    public List<MethodInfo> getMethods() {
        return methods;
    }

    /**
     * Returns the method with the specified name. If there are multiple methods
     * with that name, this method returns one of them.
     *
     * @return null if no such a method is found.
     */
    public MethodInfo getMethod(String name) {
        int n = methods.size();
        for (int i = 0; i < n; ++i) {
            MethodInfo minfo = (MethodInfo)methods.get(i);
            if (minfo.getName().equals(name)) {
              return minfo;
            }
        }

        return null;
    }

    /**
     * Just appends a method to the class.
     * It does not check method duplication or remove a bridge method.
     * Use this method only when minimizing performance overheads
     * is seriously required.
     *
     * @since 3.13
     */
    public final void addMethod(MethodInfo minfo) {
        methods.add(minfo);
    }

    /**
     * Returns all the attributes.  The returned <code>List</code> object
     * is shared with this object.  If you add a new attribute to the list,
     * the attribute is also added to the classs file represented by this
     * object.  If you remove an attribute from the list, it is also removed
     * from the class file.
     *
     * @return a list of <code>AttributeInfo</code> objects.
     * @see AttributeInfo
     */
    public List<AttributeInfo> getAttributes() {
        return attributes;
    }

    /**
     * Returns the attribute with the specified name.  If there are multiple
     * attributes with that name, this method returns either of them.   It
     * returns null if the specified attributed is not found.
     *
     * @param name          attribute name
     * @see #getAttributes()
     */
    public AttributeInfo getAttribute(String name) {
        ArrayList<AttributeInfo> list = attributes;
        int n = list.size();
        for (int i = 0; i < n; ++i) {
            AttributeInfo ai = (AttributeInfo)list.get(i);
            if (ai.getName().equals(name)) {
              return ai;
            }
        }

        return null;
    }

    /**
     * Appends an attribute. If there is already an attribute with the same
     * name, the new one substitutes for it.
     *
     * @see #getAttributes()
     */
    public void addAttribute(AttributeInfo info) {
        AttributeInfo.remove(attributes, info.getName());
        attributes.add(info);
    }

    /**
     * Returns the source file containing this class.
     *
     * @return null if this information is not available.
     */
    public String getSourceFile() {
        SourceFileAttribute sf
            = (SourceFileAttribute)getAttribute(SourceFileAttribute.tag);
        if (sf == null) {
          return null;
        } else {
          return sf.getFileName();
        }
    }

    private void read(DataInput in) throws IOException {
        int i, n;
        int magic = in.readInt();
        if (magic != 0xCAFEBABE) {
          throw new IOException("bad magic number: " + Integer.toHexString(magic));
        }

        minor = in.readUnsignedShort();
        major = in.readUnsignedShort();
        constPool = new ConstPool(in);
        accessFlags = in.readUnsignedShort();
        thisClass = in.readUnsignedShort();
        constPool.setThisClassInfo(thisClass);
        superClass = in.readUnsignedShort();
        n = in.readUnsignedShort();
        if (n == 0) {
          interfaces = null;
        } else {
            interfaces = new int[n];
            for (i = 0; i < n; ++i) {
              interfaces[i] = in.readUnsignedShort();
            }
        }

        ConstPool cp = constPool;
        n = in.readUnsignedShort();
        fields = new ArrayList<FieldInfo>();
        for (i = 0; i < n; ++i) {
          addField(new FieldInfo(cp, in));
        }

        n = in.readUnsignedShort();
        methods = new ArrayList<MethodInfo>();
        for (i = 0; i < n; ++i) {
          addMethod(new MethodInfo(cp, in));
        }

        attributes = new ArrayList<AttributeInfo>();
        n = in.readUnsignedShort();
        for (i = 0; i < n; ++i) {
          addAttribute(AttributeInfo.read(cp, in));
        }

        thisclassname = constPool.getClassInfo(thisClass);
    }

    /**
     * Writes a class file represented by this object into an output stream.
     */
    public void write(DataOutputStream out) throws IOException {
        int i, n;

        out.writeInt(0xCAFEBABE); // magic
        out.writeShort(minor); // minor version
        out.writeShort(major); // major version
        constPool.write(out); // constant pool
        out.writeShort(accessFlags);
        out.writeShort(thisClass);
        out.writeShort(superClass);

        if (interfaces == null) {
          n = 0;
        } else {
          n = interfaces.length;
        }

        out.writeShort(n);
        for (i = 0; i < n; ++i) {
          out.writeShort(interfaces[i]);
        }

        n = fields.size();
        out.writeShort(n);
        for (i = 0; i < n; ++i) {
            FieldInfo finfo = fields.get(i);
            finfo.write(out);
        }

        n = methods.size();
        out.writeShort(n);
        for (i = 0; i < n; ++i) {
            MethodInfo minfo = methods.get(i);
            minfo.write(out);
        }

        out.writeShort(attributes.size());
        AttributeInfo.writeAll(attributes, out);
    }

    /**
     * Get the Major version.
     *
     * @return the major version
     */
    public int getMajorVersion() {
        return major;
    }

    /**
     * Set the major version.
     *
     * @param major
     *            the major version
     */
    public void setMajorVersion(int major) {
        this.major = major;
    }

    /**
     * Get the minor version.
     *
     * @return the minor version
     */
    public int getMinorVersion() {
        return minor;
    }

    /**
     * Set the minor version.
     *
     * @param minor
     *            the minor version
     */
    public void setMinorVersion(int minor) {
        this.minor = minor;
    }

    /**
     * Sets the major and minor version to Java 5.
     *
     * If the major version is older than 49, Java 5
     * extensions such as annotations are ignored
     * by the JVM.
     */
    public void setVersionToJava5() {
        this.major = 49;
        this.minor = 0;
    }

    /**
     * This method is for lazy programmers only;
     * it checks both runtime and compile annotations.
     *
     * You should know what retention your annotations are set to,
     * but in case you aren't sure, this method checks them all.

     * @param name - The fully qualified annotation name
     * @return - An annotation descriptor of the type you want.
     */
    @Override
    public Annotation getAnnotation(String name) {
      AttributeInfo attr = getAttribute(AnnotationsAttribute.visibleTag);
      if (attr != null) {
        Annotation anno = ((AnnotationsAttribute)attr).getAnnotation(name);
        if (anno != null) {
          return anno;
        }
      }
      attr = getAttribute(AnnotationsAttribute.invisibleTag);
      if (attr == null) {
        return null;
      }
      return ((AnnotationsAttribute)attr).getAnnotation(name);
    }

    public Annotation getAnnotation(Class<?> annoClass) {
      Annotation anno = getAnnotation(annoClass.getName());
      if (anno == null) {
        return null;
      }
      return anno;
    }

    public boolean hasAnnotation(Class<?> annoClass) {
      Annotation anno = getAnnotation(annoClass.getName());
      return anno != null;
    }

    @SuppressWarnings("unchecked")
    public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> annoClass, ClassLoader loader) throws ClassNotFoundException, NoSuchClassError {
      Annotation anno = getAnnotation(annoClass);
      if (anno != null) {
        return (T)anno.toAnnotationType(loader, new ClassPool());
      }
      return null;
    }
    /**
     * @return all compiletime and runtime annotations present on this class file.
     */
    @Override
    public Annotation[] getAnnotations() {
      AttributeInfo vis = getAttribute(AnnotationsAttribute.visibleTag);
      AttributeInfo invis = getAttribute(AnnotationsAttribute.invisibleTag);
      return BytecodeUtil.extractAnnotations((AnnotationsAttribute)vis, (AnnotationsAttribute)invis);
    }

    /**
     * This method returns any runtime annotations on the given classfile.
     *
     * @param name - The fully qualified annotation name
     * @return - An annotation descriptor of the type you want.
     */
    public Annotation getRuntimeAnnotation(String name) {
      AttributeInfo attr = getAttribute(AnnotationsAttribute.visibleTag);
      if (attr == null) {
        return null;
      }
      return ((AnnotationsAttribute)attr).getAnnotation(name);
    }

    /**
     * This method returns any compile annotations on the given classfile.
     *
     * @param name - The fully qualified annotation name
     * @return - An annotation descriptor of the type you want.
     */
    public Annotation getCompileAnnotation(String name) {
      if (name.charAt(0) != '@') {
        name = '@' + name;
      }
      AttributeInfo attr = getAttribute(AnnotationsAttribute.invisibleTag);
      if (attr == null) {
        return null;
      }
      return ((AnnotationsAttribute)attr).getAnnotation(name);
    }

    public String getPackage() {
      return X_Modifier.sourceNameToPackage(constPool.getClassName());
    }

    public String getEnclosedName() {
      return X_Modifier.sourceNameToEnclosed(constPool.getClassName());
    }

    public String getSimpleName() {
      String enclosed = getEnclosedName();
      int last = enclosed.lastIndexOf('.');
      return last == -1 ? enclosed : enclosed.substring(last + 1);
    }

    public String getQualifiedName() {
      return X_Source.qualifiedName(getPackage(), getEnclosedName());
    }

    @Override
    public int hashCode() {
      return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ClassFile)) {
        return false;
      }
      ClassFile other = (ClassFile)obj;
      if (!getName().equals(other.getName())) {
        return false;
      }
      // Classes of the same name and different location are not equal
      return X_Util.equal(getSourceFile(), other.getSourceFile());

    }

    @Override
    public String toString() {
      return X_Modifier.classModifiers(getAccessFlags())+getName();
    }

    public String getResourceName() {
      return getPackage().replace('.', File.separatorChar)+File.separatorChar+getSourceFile();
    }

    public boolean isClass(String pkg, String enclosed) {
      return getPackage().equals(pkg) && getEnclosedName().equals(enclosed);
    }

    public boolean hasSuperClass(String superClass) {
      return getSuperclass().equals(superClass);
    }

}

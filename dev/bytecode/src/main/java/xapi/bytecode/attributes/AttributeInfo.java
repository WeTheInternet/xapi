package xapi.bytecode.attributes;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import xapi.bytecode.ConstPool;
import xapi.bytecode.annotation.AnnotationDefaultAttribute;
import xapi.bytecode.annotation.AnnotationsAttribute;


public class AttributeInfo {
  protected ConstPool constPool;
  protected int name;
  protected byte[] info;

  protected AttributeInfo(ConstPool cp, int attrname, byte[] attrinfo) {
      constPool = cp;
      name = attrname;
      info = attrinfo;
  }

  protected AttributeInfo(ConstPool cp, String attrname) {
      this(cp, attrname, (byte[])null);
  }

  /**
   * Constructs an <code>attribute_info</code> structure.
   *
   * @param cp                constant pool table
   * @param attrname          attribute name
   * @param attrinfo          <code>info</code> field
   *                          of <code>attribute_info</code> structure.
   */
  public AttributeInfo(ConstPool cp, String attrname, byte[] attrinfo) {
      this(cp, cp.addUtf8Info(attrname), attrinfo);
  }

  protected AttributeInfo(ConstPool cp, int n, DataInput in)
      throws IOException
  {
      constPool = cp;
      name = n;
      int len = in.readInt();
      info = new byte[len];
      if (len > 0)
          in.readFully(info);
  }

  public static AttributeInfo read(ConstPool cp, DataInput in)
      throws IOException
  {
      int name = in.readUnsignedShort();
      String nameStr = cp.getUtf8Info(name);
      if (nameStr.charAt(0) < 'L') {
          if (nameStr.equals(AnnotationDefaultAttribute.tag))
              return new AnnotationDefaultAttribute(cp, name, in);
          else if (nameStr.equals(ConstantAttribute.tag))
              return new ConstantAttribute(cp, name, in);
          else if (nameStr.equals(InnerClassesAttribute.tag))
              return new InnerClassesAttribute(cp, name, in);
      }
      else {
          /* Note that the names of Annotations attributes begin with 'R'.
           */
         if (nameStr.equals(AnnotationsAttribute.visibleTag)
                   || nameStr.equals(AnnotationsAttribute.invisibleTag)) {
              // RuntimeVisibleAnnotations or RuntimeInvisibleAnnotations
              return new AnnotationsAttribute(cp, name, in);
          }
          else if (nameStr.equals(SignatureAttribute.tag))
              return new SignatureAttribute(cp, name, in);
          else if (nameStr.equals(SourceFileAttribute.tag))
              return new SourceFileAttribute(cp, name, in);
      }

      return new AttributeInfo(cp, name, in);
  }

  /**
   * Returns an attribute name.
   */
  public String getName() {
      return constPool.getUtf8Info(name);
  }

  /**
   * Returns a constant pool table.
   */
  public ConstPool getConstPool() { return constPool; }

  /**
   * Returns the length of this <code>attribute_info</code>
   * structure.
   * The returned value is <code>attribute_length + 6</code>.
   */
  public int length() {
      return info.length + 6;
  }

  /**
   * Returns the <code>info</code> field
   * of this <code>attribute_info</code> structure.
   *
   * <p>This method is not available if the object is an instance
   * of <code>CodeAttribute</code>.
   */
  public byte[] get() { return info; }

  /**
   * Sets the <code>info</code> field
   * of this <code>attribute_info</code> structure.
   *
   * <p>This method is not available if the object is an instance
   * of <code>CodeAttribute</code>.
   */
  public void set(byte[] newinfo) { info = newinfo; }

  /**
   * Makes a copy.  Class names are replaced according to the
   * given <code>Map</code> object.
   *
   * @param newCp     the constant pool table used by the new copy.
   * @param classnames        pairs of replaced and substituted
   *                          class names.
   */
  public AttributeInfo copy(ConstPool newCp, Map<?, ?> classnames) {
      int s = info.length;
      byte[] srcInfo = info;
      byte[] newInfo = new byte[s];
      for (int i = 0; i < s; ++i)
          newInfo[i] = srcInfo[i];

      return new AttributeInfo(newCp, getName(), newInfo);
  }

  void write(DataOutputStream out) throws IOException {
      out.writeShort(name);
      out.writeInt(info.length);
      if (info.length > 0)
          out.write(info);
  }

  public static int getLength(ArrayList<?> list) {
      int size = 0;
      int n = list.size();
      for (int i = 0; i < n; ++i) {
          AttributeInfo attr = (AttributeInfo)list.get(i);
          size += attr.length();
      }

      return size;
  }

  public static AttributeInfo lookup(ArrayList<?> list, String name) {
      if (list == null)
          return null;

      ListIterator<?> iterator = list.listIterator();
      while (iterator.hasNext()) {
          AttributeInfo ai = (AttributeInfo)iterator.next();
          if (ai.getName().equals(name))
              return ai;
      }

      return null;            // no such attribute
  }

  public static synchronized void remove(ArrayList<?> list, String name) {
      if (list == null)
          return;

      ListIterator<?> iterator = list.listIterator();
      while (iterator.hasNext()) {
          AttributeInfo ai = (AttributeInfo)iterator.next();
          if (ai.getName().equals(name))
              iterator.remove();
      }
  }

  public static void writeAll(ArrayList<?> list, DataOutputStream out)
      throws IOException
  {
      if (list == null)
          return;

      int n = list.size();
      for (int i = 0; i < n; ++i) {
          AttributeInfo attr = (AttributeInfo)list.get(i);
          attr.write(out);
      }
  }

  public static ArrayList<AttributeInfo> copyAll(ArrayList<?> list, ConstPool cp) {
      if (list == null)
          return null;

      ArrayList<AttributeInfo> newList = new ArrayList<AttributeInfo>();
      int n = list.size();
      for (int i = 0; i < n; ++i) {
          AttributeInfo attr = (AttributeInfo)list.get(i);
          newList.add(attr.copy(cp, null));
      }

      return newList;
  }

  /* The following two methods are used to implement
   * ClassFile.renameClass().
   * Only CodeAttribute and LocalVariableAttribute override
   * this method.
   */
  void renameClass(String oldname, String newname) {}
  void renameClass(Map<?, ?> classnames) {}

  public static void renameClass(List<?> attributes, String oldname, String newname) {
      Iterator<?> iterator = attributes.iterator();
      while (iterator.hasNext()) {
          AttributeInfo ai = (AttributeInfo)iterator.next();
          ai.renameClass(oldname, newname);
      }
  }

  public static void renameClass(List<?> attributes, Map<?, ?> classnames) {
      Iterator<?> iterator = attributes.iterator();
      while (iterator.hasNext()) {
          AttributeInfo ai = (AttributeInfo)iterator.next();
          ai.renameClass(classnames);
      }
  }
}

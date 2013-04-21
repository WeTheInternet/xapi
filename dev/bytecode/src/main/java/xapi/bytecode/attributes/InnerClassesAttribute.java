package xapi.bytecode.attributes;

import java.io.DataInput;
import java.io.IOException;
import java.util.Map;

import xapi.bytecode.ConstPool;
import xapi.util.X_Byte;

public class InnerClassesAttribute extends AttributeInfo {
    /**
     * The name of this attribute <code>"InnerClasses"</code>.
     */
    public static final String tag = "InnerClasses";

    InnerClassesAttribute(ConstPool cp, int n, DataInput in)
        throws IOException
    {
        super(cp, n, in);
    }

    private InnerClassesAttribute(ConstPool cp, byte[] info) {
        super(cp, tag, info);
    }

    /**
     * Constructs an empty InnerClasses attribute.
     *
     * @see #append(String, String, String, int)
     */
    public InnerClassesAttribute(ConstPool cp) {
        super(cp, tag, new byte[2]);
        X_Byte.write16bit(0, get(), 0);
    }

    /**
     * Returns <code>number_of_classes</code>.
     */
    public int tableLength() { return X_Byte.readU16bit(get(), 0); }

    /**
     * Returns <code>classes[nth].inner_class_info_index</code>.
     */
    public int innerClassIndex(int nth) {
        return X_Byte.readU16bit(get(), nth * 8 + 2);
    }

    /**
     * Returns the class name indicated
     * by <code>classes[nth].inner_class_info_index</code>.
     *
     * @return null or the class name.
     */
    public String innerClass(int nth) {
        int i = innerClassIndex(nth);
        if (i == 0)
            return null;
        else
            return constPool.getClassInfo(i);
    }

    /**
     * Sets <code>classes[nth].inner_class_info_index</code> to
     * the given index.
     */
    public void setInnerClassIndex(int nth, int index) {
        X_Byte.write16bit(index, get(), nth * 8 + 2);
    }

    /**
     * Returns <code>classes[nth].outer_class_info_index</code>.
     */
    public int outerClassIndex(int nth) {
        return X_Byte.readU16bit(get(), nth * 8 + 4);
    }

    /**
     * Returns the class name indicated
     * by <code>classes[nth].outer_class_info_index</code>.
     *
     * @return null or the class name.
     */
    public String outerClass(int nth) {
        int i = outerClassIndex(nth);
        if (i == 0)
            return null;
        else
            return constPool.getClassInfo(i);
    }

    /**
     * Sets <code>classes[nth].outer_class_info_index</code> to
     * the given index.
     */
    public void setOuterClassIndex(int nth, int index) {
        X_Byte.write16bit(index, get(), nth * 8 + 4);
    }

    /**
     * Returns <code>classes[nth].inner_name_index</code>.
     */
    public int innerNameIndex(int nth) {
        return X_Byte.readU16bit(get(), nth * 8 + 6);
    }

    /**
     * Returns the simple class name indicated
     * by <code>classes[nth].inner_name_index</code>.
     *
     * @return null or the class name.
     */
    public String innerName(int nth) {
        int i = innerNameIndex(nth);
        if (i == 0)
            return null;
        else
            return constPool.getUtf8Info(i);
    }

    /**
     * Sets <code>classes[nth].inner_name_index</code> to
     * the given index.
     */
    public void setInnerNameIndex(int nth, int index) {
        X_Byte.write16bit(index, get(), nth * 8 + 6);
    }

    /**
     * Returns <code>classes[nth].inner_class_access_flags</code>.
     */
    public int accessFlags(int nth) {
        return X_Byte.readU16bit(get(), nth * 8 + 8);
    }

    /**
     * Sets <code>classes[nth].inner_class_access_flags</code> to
     * the given index.
     */
    public void setAccessFlags(int nth, int flags) {
        X_Byte.write16bit(flags, get(), nth * 8 + 8);
    }

    /**
     * Appends a new entry.
     *
     * @param inner     <code>inner_class_info_index</code>
     * @param outer     <code>outer_class_info_index</code>
     * @param name      <code>inner_name_index</code>
     * @param flags     <code>inner_class_access_flags</code>
     */
    public void append(String inner, String outer, String name, int flags) {
        int i = constPool.addClassInfo(inner);
        int o = constPool.addClassInfo(outer);
        int n = constPool.addUtf8Info(name);
        append(i, o, n, flags);
    }

    /**
     * Appends a new entry.
     *
     * @param inner     <code>inner_class_info_index</code>
     * @param outer     <code>outer_class_info_index</code>
     * @param name      <code>inner_name_index</code>
     * @param flags     <code>inner_class_access_flags</code>
     */
    public void append(int inner, int outer, int name, int flags) {
        byte[] data = get();
        int len = data.length;
        byte[] newData = new byte[len + 8];
        for (int i = 2; i < len; ++i)
            newData[i] = data[i];

        int n = X_Byte.readU16bit(data, 0);
        X_Byte.write16bit(n + 1, newData, 0);

        X_Byte.write16bit(inner, newData, len);
        X_Byte.write16bit(outer, newData, len + 2);
        X_Byte.write16bit(name, newData, len + 4);
        X_Byte.write16bit(flags, newData, len + 6);

        set(newData);
    }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp     the constant pool table used by the new copy.
     * @param classnames        pairs of replaced and substituted
     *                          class names.
     */
    @Override
    public AttributeInfo copy(ConstPool newCp, Map<?, ?> classnames) {
        byte[] src = get();
        byte[] dest = new byte[src.length];
        ConstPool cp = getConstPool();
        InnerClassesAttribute attr = new InnerClassesAttribute(newCp, dest);
        int n = X_Byte.readU16bit(src, 0);
        X_Byte.write16bit(n, dest, 0);
        int j = 2;
        for (int i = 0; i < n; ++i) {
            int innerClass = X_Byte.readU16bit(src, j);
            int outerClass = X_Byte.readU16bit(src, j + 2);
            int innerName = X_Byte.readU16bit(src, j + 4);
            int innerAccess = X_Byte.readU16bit(src, j + 6);

            if (innerClass != 0)
                innerClass = cp.copy(innerClass, newCp, classnames);

            X_Byte.write16bit(innerClass, dest, j);

            if (outerClass != 0)
                outerClass = cp.copy(outerClass, newCp, classnames);

            X_Byte.write16bit(outerClass, dest, j + 2);

            if (innerName != 0)
                innerName = cp.copy(innerName, newCp, classnames);

            X_Byte.write16bit(innerName, dest, j + 4);
            X_Byte.write16bit(innerAccess, dest, j + 6);
            j += 8;
        }

        return attr;
    }
}

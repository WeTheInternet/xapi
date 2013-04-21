package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;
import xapi.bytecode.Descriptor;


public class ClassMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a class value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index the index of a CONSTANT_Utf8_info structure.
     */
    public ClassMemberValue(int index, ConstPool cp) {
        super('c', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a class value.
     *
     * @param className         the initial value.
     */
    public ClassMemberValue(String className, ConstPool cp) {
        super('c', cp);
        setValue(className);
    }

    /**
     * Constructs a class value.
     * The initial value is java.lang.Class.
     */
    public ClassMemberValue(ConstPool cp) {
        super('c', cp);
        setValue("java.lang.Class");
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m)
            throws ClassNotFoundException {
        final String classname = getValue();
        if (classname.equals("void"))
            return void.class;
        else if (classname.equals("int"))
            return int.class;
        else if (classname.equals("byte"))
            return byte.class;
        else if (classname.equals("long"))
            return long.class;
        else if (classname.equals("double"))
            return double.class;
        else if (classname.equals("float"))
            return float.class;
        else if (classname.equals("char"))
            return char.class;
        else if (classname.equals("short"))
            return short.class;
        else if (classname.equals("boolean"))
            return boolean.class;
        else
            return loadClass(cl, classname);
    }

    @Override
    Class<?> getType(ClassLoader cl) throws ClassNotFoundException {
        return loadClass(cl, "java.lang.Class");
    }

    /**
     * Obtains the value of the member.
     *
     * @return fully-qualified class name.
     */
    public String getValue() {
        String v = cp.getUtf8Info(valueIndex);
        return Descriptor.toClassName(v);
    }

    /**
     * Sets the value of the member.
     *
     * @param newClassName      fully-qualified class name.
     */
    public void setValue(String newClassName) {
        String setTo = Descriptor.of(newClassName);
        valueIndex = cp.addUtf8Info(setTo);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return "<" + getValue() + " class>";
    }

    /**
     * Writes the value.
     */
    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.classInfoIndex(cp.getUtf8Info(valueIndex));
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitClassMemberValue(this);
    }
}

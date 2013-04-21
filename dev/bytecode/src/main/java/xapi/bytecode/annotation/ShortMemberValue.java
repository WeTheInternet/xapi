package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;

public class ShortMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a short constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Integer_info structure.
     */
    public ShortMemberValue(int index, ConstPool cp) {
        super('S', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a short constant value.
     *
     * @param s         the initial value.
     */
    public ShortMemberValue(short s, ConstPool cp) {
        super('S', cp);
        setValue(s);
    }

    /**
     * Constructs a short constant value.  The initial value is 0.
     */
    public ShortMemberValue(ConstPool cp) {
        super('S', cp);
        setValue((short)0);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Short(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return short.class;
    }

    /**
     * Obtains the value of the member.
     */
    public short getValue() {
        return (short)cp.getIntegerInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(short newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return Short.toString(getValue());
    }

    /**
     * Writes the value.
     */
    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.constValueIndex(getValue());
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitShortMemberValue(this);
    }
}

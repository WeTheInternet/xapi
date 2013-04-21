package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;

public class ByteMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a byte constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Integer_info structure.
     */
    public ByteMemberValue(int index, ConstPool cp) {
        super('B', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a byte constant value.
     *
     * @param b         the initial value.
     */
    public ByteMemberValue(byte b, ConstPool cp) {
        super('B', cp);
        setValue(b);
    }

    /**
     * Constructs a byte constant value.  The initial value is 0.
     */
    public ByteMemberValue(ConstPool cp) {
        super('B', cp);
        setValue((byte)0);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Byte(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return byte.class;
    }

    /**
     * Obtains the value of the member.
     */
    public byte getValue() {
        return (byte)cp.getIntegerInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(byte newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return Byte.toString(getValue());
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
        visitor.visitByteMemberValue(this);
    }
}

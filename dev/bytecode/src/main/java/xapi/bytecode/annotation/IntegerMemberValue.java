package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;

public class IntegerMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs an int constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Integer_info structure.
     */
    public IntegerMemberValue(int index, ConstPool cp) {
        super('I', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs an int constant value.
     * Note that this constructor receives <b>the initial value
     * as the second parameter</b>
     * unlike the corresponding constructors in the sibling classes.
     * This is for making a difference from the constructor that receives
     * an index into the constant pool table as the first parameter.
     * Note that the index is also int type.
     *
     * @param value         the initial value.
     */
    public IntegerMemberValue(ConstPool cp, int value) {
        super('I', cp);
        setValue(value);
    }

    /**
     * Constructs an int constant value.  The initial value is 0.
     */
    public IntegerMemberValue(ConstPool cp) {
        super('I', cp);
        setValue(0);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Integer(getValue());
    }

    @Override
    public Class<?> getType(ClassLoader cl) {
        return int.class;
    }

    /**
     * Obtains the value of the member.
     */
    public int getValue() {
        return cp.getIntegerInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(int newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return Integer.toString(getValue());
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
        visitor.visitIntegerMemberValue(this);
    }
}

package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;

public class BooleanMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a boolean constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Integer_info structure.
     */
    public BooleanMemberValue(int index, ConstPool cp) {
        super('Z', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a boolean constant value.
     *
     * @param b         the initial value.
     */
    public BooleanMemberValue(boolean b, ConstPool cp) {
        super('Z', cp);
        setValue(b);
    }

    /**
     * Constructs a boolean constant value.  The initial value is false.
     */
    public BooleanMemberValue(ConstPool cp) {
        super('Z', cp);
        setValue(false);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Boolean(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return boolean.class;
    }

    /**
     * Obtains the value of the member.
     */
    public boolean getValue() {
        return cp.getIntegerInfo(valueIndex) != 0;
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(boolean newValue) {
        valueIndex = cp.addIntegerInfo(newValue ? 1 : 0);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return getValue() ? "true" : "false";
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
        visitor.visitBooleanMemberValue(this);
    }
}

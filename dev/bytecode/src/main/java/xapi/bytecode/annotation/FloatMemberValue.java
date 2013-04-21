package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;

public class FloatMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a float constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Float_info structure.
     */
    public FloatMemberValue(int index, ConstPool cp) {
        super('F', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a float constant value.
     *
     * @param f         the initial value.
     */
    public FloatMemberValue(float f, ConstPool cp) {
        super('F', cp);
        setValue(f);
    }

    /**
     * Constructs a float constant value.  The initial value is 0.0.
     */
    public FloatMemberValue(ConstPool cp) {
        super('F', cp);
        setValue(0.0F);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Float(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return float.class;
    }

    /**
     * Obtains the value of the member.
     */
    public float getValue() {
        return cp.getFloatInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(float newValue) {
        valueIndex = cp.addFloatInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return Float.toString(getValue());
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
        visitor.visitFloatMemberValue(this);
    }
}

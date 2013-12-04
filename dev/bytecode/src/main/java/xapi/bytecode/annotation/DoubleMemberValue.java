package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;

public class DoubleMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a double constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Double_info structure.
     */
    public DoubleMemberValue(int index, ConstPool cp) {
        super('D', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a double constant value.
     *
     * @param d     the initial value.
     */
    public DoubleMemberValue(double d, ConstPool cp) {
        super('D', cp);
        setValue(d);
    }

    /**
     * Constructs a double constant value.  The initial value is 0.0.
     */
    public DoubleMemberValue(ConstPool cp) {
        super('D', cp);
        setValue(0.0);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Double(getValue());
    }

    @Override
    public Class<?> getType(ClassLoader cl) {
        return double.class;
    }

    /**
     * Obtains the value of the member.
     */
    public double getValue() {
        return cp.getDoubleInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(double newValue) {
        valueIndex = cp.addDoubleInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return Double.toString(getValue());
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
        visitor.visitDoubleMemberValue(this);
    }
}

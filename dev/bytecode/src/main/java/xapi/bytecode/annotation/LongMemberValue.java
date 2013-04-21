package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;

public class LongMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a long constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Long_info structure.
     */
    public LongMemberValue(int index, ConstPool cp) {
        super('J', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a long constant value.
     *
     * @param j         the initial value.
     */
    public LongMemberValue(long j, ConstPool cp) {
        super('J', cp);
        setValue(j);
    }

    /**
     * Constructs a long constant value.  The initial value is 0.
     */
    public LongMemberValue(ConstPool cp) {
        super('J', cp);
        setValue(0L);
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Long(getValue());
    }

    @Override
    Class<?> getType(ClassLoader cl) {
        return long.class;
    }

    /**
     * Obtains the value of the member.
     */
    public long getValue() {
        return cp.getLongInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(long newValue) {
        valueIndex = cp.addLongInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return Long.toString(getValue());
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
        visitor.visitLongMemberValue(this);
    }
}

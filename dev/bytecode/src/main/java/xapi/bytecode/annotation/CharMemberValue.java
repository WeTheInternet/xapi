package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;

public class CharMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a char constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Integer_info structure.
     */
    public CharMemberValue(int index, ConstPool cp) {
        super('C', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a char constant value.
     *
     * @param c     the initial value.
     */
    public CharMemberValue(char c, ConstPool cp) {
        super('C', cp);
        setValue(c);
    }

    /**
     * Constructs a char constant value.  The initial value is '\0'.
     */
    public CharMemberValue(ConstPool cp) {
        super('C', cp);
        setValue('\0');
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return new Character(getValue());
    }

    @Override
    public Class<?> getType(ClassLoader cl) {
        return char.class;
    }

    /**
     * Obtains the value of the member.
     */
    public char getValue() {
        return (char)cp.getIntegerInfo(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(char newValue) {
        valueIndex = cp.addIntegerInfo(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return Character.toString(getValue());
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
        visitor.visitCharMemberValue(this);
    }
}

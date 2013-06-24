package xapi.bytecode.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import xapi.bytecode.ClassPool;
import xapi.bytecode.ConstPool;

public class AnnotationMemberValue extends MemberValue {
    Annotation value;

    /**
     * Constructs an annotation member.  The initial value is not specified.
     */
    public AnnotationMemberValue(ConstPool cp) {
        this(null, cp);
    }

    /**
     * Constructs an annotation member.  The initial value is specified by
     * the first parameter.
     */
    public AnnotationMemberValue(Annotation a, ConstPool cp) {
        super('@', cp);
        value = a;
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m)
        throws ClassNotFoundException
    {
        return AnnotationImpl.make(cl, getType(cl), cp, value);
    }

    @Override
    Class<?> getType(ClassLoader cl) throws ClassNotFoundException {
        if (value == null)
            throw new ClassNotFoundException("no type specified");
        else
            return loadClass(cl, value.getTypeName());
    }

    /**
     * Obtains the value.
     */
    public Annotation getValue() {
        return value;
    }

    /**
     * Sets the value of this member.
     */
    public void setValue(Annotation newValue) {
        value = newValue;
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return value.toString();
    }

    /**
     * Writes the value.
     */
    @Override
    public void write(AnnotationsWriter writer) throws IOException {
        writer.annotationValue();
        value.write(writer);
    }

    /**
     * Accepts a visitor.
     */
    @Override
    public void accept(MemberValueVisitor visitor) {
        visitor.visitAnnotationMemberValue(this);
    }
}
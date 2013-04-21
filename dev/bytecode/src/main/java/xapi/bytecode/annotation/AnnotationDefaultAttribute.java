package xapi.bytecode.annotation;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.util.Map;

import xapi.bytecode.ConstPool;
import xapi.bytecode.CtClass;
import xapi.bytecode.attributes.AttributeInfo;

public class AnnotationDefaultAttribute extends AttributeInfo {
    /**
     * The name of the <code>AnnotationDefault</code> attribute.
     */
    public static final String tag = "AnnotationDefault";

    /**
     * Constructs an <code>AnnotationDefault_attribute</code>.
     *
     * @param cp            constant pool
     * @param info          the contents of this attribute.  It does not
     *                      include <code>attribute_name_index</code> or
     *                      <code>attribute_length</code>.
     */
    public AnnotationDefaultAttribute(ConstPool cp, byte[] info) {
        super(cp, tag, info);
    }

    /**
     * Constructs an empty <code>AnnotationDefault_attribute</code>.
     * The default value can be set by <code>setDefaultValue()</code>.
     *
     * @param cp            constant pool
     * @see #setDefaultValue(javassist.bytecode.annotation.MemberValue)
     */
    public AnnotationDefaultAttribute(ConstPool cp) {
        this(cp, new byte[] { 0, 0 });
    }

    /**
     * @param n     the attribute name.
     */
    public AnnotationDefaultAttribute(ConstPool cp, int n, DataInput in)
        throws IOException
    {
        super(cp, n, in);
    }

    /**
     * Copies this attribute and returns a new copy.
     */
    @Override
    public AttributeInfo copy(ConstPool newCp, Map<?, ?> classnames) {
        AnnotationsAttribute.Copier copier
            = new AnnotationsAttribute.Copier(info, constPool, newCp, classnames);
        try {
            copier.memberValue(0);
            return new AnnotationDefaultAttribute(newCp, copier.close());
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Obtains the default value represented by this attribute.
     */
    public MemberValue getDefaultValue()
    {
       try {
           return new AnnotationsAttribute.Parser(info, constPool)
                                          .parseMemberValue();
       }
       catch (Exception e) {
           throw new RuntimeException(e.toString());
       }
    }

    /**
     * Changes the default value represented by this attribute.
     *
     * @param value         the new value.
     * @see javassist.bytecode.annotation.Annotation#createMemberValue(ConstPool, CtClass)
     */
    public void setDefaultValue(MemberValue value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AnnotationsWriter writer = new AnnotationsWriter(output, constPool);
        try {
            value.write(writer);
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);      // should never reach here.
        }

        set(output.toByteArray());

    }

    /**
     * Returns a string representation of this object.
     */
    @Override
    public String toString() {
        return getDefaultValue().toString();
    }
}

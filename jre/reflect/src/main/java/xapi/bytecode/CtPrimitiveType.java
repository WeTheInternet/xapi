package xapi.bytecode;

import xapi.source.Modifier;

public final class CtPrimitiveType extends CtClass {
    private final char descriptor;
    private final String wrapperName;
    private final String getMethodName;
    private final String mDescriptor;
    private final int dataSize;

    CtPrimitiveType(String name, char desc, String wrapper,
                    String methodName, String mDesc, int size) {
        super(name);
        descriptor = desc;
        wrapperName = wrapper;
        getMethodName = methodName;
        mDescriptor = mDesc;
        dataSize = size;
    }

    /**
     * Returns <code>true</code> if this object represents a primitive
     * Java type: boolean, byte, char, short, int, long, float, double,
     * or void.
     */
    @Override
    public boolean isPrimitive() { return true; }

    /**
     * Returns the modifiers for this type.
     * For decoding, use <code>javassist.Modifier</code>.
     *
     * @see Modifier
     */
    @Override
    public int getModifiers() {
        return Modifier.PUBLIC | Modifier.FINAL;
    }

    /**
     * Returns the descriptor representing this type.
     * For example, if the type is int, then the descriptor is I.
     */
    public char getDescriptor() { return descriptor; }

    /**
     * Returns the name of the wrapper class.
     * For example, if the type is int, then the wrapper class is
     * <code>java.lang.Integer</code>.
     */
    public String getWrapperName() { return wrapperName; }

    /**
     * Returns the name of the method for retrieving the value
     * from the wrapper object.
     * For example, if the type is int, then the method name is
     * <code>intValue</code>.
     */
    public String getGetMethodName() { return getMethodName; }

    /**
     * Returns the descriptor of the method for retrieving the value
     * from the wrapper object.
     * For example, if the type is int, then the method descriptor is
     * <code>()I</code>.
     */
    public String getGetMethodDescriptor() { return mDescriptor; }

    /**
     * Returns the data size of the primitive type.
     * If the type is long or double, this method returns 2.
     * Otherwise, it returns 1.
     */
    public int getDataSize() { return dataSize; }
}

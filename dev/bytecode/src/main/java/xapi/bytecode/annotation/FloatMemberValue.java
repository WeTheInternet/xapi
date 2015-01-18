/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * MODIFIED BY James Nelson of We The Internet, 2013.
 * Repackaged to avoid conflicts with different versions of Javassist,
 * and modified Javassist APIs to make them more accessible to outside code.
 */
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
    public Class<?> getType(ClassLoader cl) {
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

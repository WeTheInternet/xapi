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

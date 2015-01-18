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
    public Class<?> getType(ClassLoader cl) {
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

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
    public Class<?> getType(ClassLoader cl) {
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

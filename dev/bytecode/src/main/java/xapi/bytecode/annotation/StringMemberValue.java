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

public class StringMemberValue extends MemberValue {
    int valueIndex;

    /**
     * Constructs a string constant value.  The initial value is specified
     * by the constant pool entry at the given index.
     *
     * @param index     the index of a CONSTANT_Utf8_info structure.
     */
    public StringMemberValue(int index, ConstPool cp) {
        super('s', cp);
        this.valueIndex = index;
    }

    /**
     * Constructs a string constant value.
     *
     * @param str         the initial value.
     */
    public StringMemberValue(String str, ConstPool cp) {
        super('s', cp);
        setValue(str);
    }

    /**
     * Constructs a string constant value.  The initial value is "".
     */
    public StringMemberValue(ConstPool cp) {
        super('s', cp);
        setValue("");
    }

    @Override
    Object getValue(ClassLoader cl, ClassPool cp, Method m) {
        return getValue();
    }

    @Override
    public Class<?> getType(ClassLoader cl) {
        return String.class;
    }

    /**
     * Obtains the value of the member.
     */
    public String getValue() {
        return cp.getUtf8Info(valueIndex);
    }

    /**
     * Sets the value of the member.
     */
    public void setValue(String newValue) {
        valueIndex = cp.addUtf8Info(newValue);
    }

    /**
     * Obtains the string representation of this object.
     */
    @Override
    public String toString() {
        return "\"" + getValue() + "\"";
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
        visitor.visitStringMemberValue(this);
    }
}
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
    public Class<?> getType(ClassLoader cl) throws ClassNotFoundException {
        if (value == null) {
          throw new ClassNotFoundException("no type specified");
        } else {
          return loadClass(cl, value.getTypeName());
        }
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
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
package xapi.bytecode;

import xapi.source.X_Modifier;

class CtNewClass extends CtClassType {
    /* true if the class is an interface.
     */
    protected boolean hasConstructor;

    CtNewClass(String name, ClassPool cp,
               boolean isInterface, CtClass superclass) {
        super(name, cp);
        wasChanged = true;
        String superName;
        if (isInterface || superclass == null) {
          superName = null;
        } else {
          superName = superclass.getName();
        }

        classfile = new ClassFile(isInterface, name, superName);
        if (isInterface && superclass != null) {
          classfile.setInterfaces(new String[] { superclass.getName() });
        }

        setModifiers(X_Modifier.setPublic(getModifiers()));
        hasConstructor = isInterface;
    }

    @Override
    protected void extendToString(StringBuffer buffer) {
        if (hasConstructor) {
          buffer.append("hasConstructor ");
        }

        super.extendToString(buffer);
    }

    @Override
    public void addConstructor(CtConstructor c)
        throws CannotCompileException
    {
        hasConstructor = true;
        super.addConstructor(c);
    }

}

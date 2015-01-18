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


final class CtArray extends CtClass {
    protected ClassPool pool;

    // the name of array type ends with "[]".
    CtArray(String name, ClassPool cp) {
        super(name);
        pool = cp;
    }

    @Override
    public ClassPool getClassPool() {
        return pool;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    private CtClass[] interfaces = null;

    @Override
    public int getModifiers() {
        int mod = X_Modifier.FINAL;
        try {
            mod |= getComponentType().getModifiers()
                   & (X_Modifier.PROTECTED | X_Modifier.PUBLIC | X_Modifier.PRIVATE);
        }
        catch (NotFoundException e) {}
        return mod;
    }

    @Override
    public CtClass[] getInterfaces() throws NotFoundException {
        if (interfaces == null) {
          interfaces = new CtClass[] {
              pool.get("java.lang.Cloneable"), pool.get("java.io.Serializable") };
        }

        return interfaces;
    }

    @Override
    public boolean subtypeOf(CtClass clazz) throws NotFoundException {
        if (super.subtypeOf(clazz)) {
          return true;
        }

        String cname = clazz.getName();
        if (cname.equals(javaLangObject)
            || cname.equals("java.lang.Cloneable")
            || cname.equals("java.io.Serializable")) {
          return true;
        }

        return clazz.isArray()
            && getComponentType().subtypeOf(clazz.getComponentType());
    }

    @Override
    public CtClass getComponentType() throws NotFoundException {
        String name = getName();
        return pool.get(name.substring(0, name.length() - 2));
    }

    @Override
    public CtClass getSuperclass() throws NotFoundException {
        return pool.get(javaLangObject);
    }

//    @Override
//    public CtMethod[] getMethods() {
//        try {
//            return getSuperclass().getMethods();
//        }
//        catch (NotFoundException e) {
//            return super.getMethods();
//        }
//    }
//
//    @Override
//    public CtMethod getMethod(String name, String desc)
//        throws NotFoundException
//    {
//        return getSuperclass().getMethod(name, desc);
//    }
//
//    @Override
//    public CtConstructor[] getConstructors() {
//        try {
//            return getSuperclass().getConstructors();
//        }
//        catch (NotFoundException e) {
//            return super.getConstructors();
//        }
//    }
}

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

import java.io.InputStream;
import java.net.URL;

public class ClassClassPath implements ClassPath {
    private Class<?> thisClass;

    /** Creates a search path.
     *
     * @param c     the <code>Class</code> object used to obtain a class
     *              file.  <code>getResourceAsStream()</code> is called on
     *              this object.
     */
    public ClassClassPath(Class<?> c) {
        thisClass = c;
    }

    ClassClassPath() {
        /* The value of thisClass was this.getClass() in early versions:
         *
         *     thisClass = this.getClass();
         *
         * However, this made openClassfile() not search all the system
         * class paths if javassist.jar is put in jre/lib/ext/
         * (with JDK1.4).
         */
        this(java.lang.Object.class);
    }
//    private static final boolean OLD = System.getProperty("java.version").startsWith("1.");
    private static final boolean OLD = true;//System.getProperty("java.version").startsWith("1.");
    private static final String RESOURCE_PREFIX = OLD ? "/" : "";

    /**
     * Obtains a class file by <code>getResourceAsStream()</code>.
     */
    @Override
    public InputStream openClassfile(String classname) {
        String jarname = RESOURCE_PREFIX + classname.replace('.', '/') + ".class";
        InputStream result = thisClass.getResourceAsStream(jarname);
        if (result == null) {
            // for java9+
            try {
                result = Class.forName(classname).getResourceAsStream(jarname);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return result;
    }

    /**
     * Obtains the URL of the specified class file.
     *
     * @return null if the class file could not be found.
     */
    @Override
    public URL find(String classname) {
        String jarname = "/" + classname.replace('.', '/') + ".class";
        URL result = thisClass.getResource(jarname);
        if (result == null) {
            // for java9+
            try {
                result = Class.forName(classname).getResource(jarname);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return result;
    }

    /**
     * Does nothing.
     */
    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return thisClass.getName() + ".class";
    }
}

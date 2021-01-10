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

import xapi.fu.Filter.Filter1;
import xapi.source.X_Source;

import java.lang.annotation.Annotation;

public class ClassFileMatchers {

  public static class HasAnnotationMatcher implements Filter1<ClassFile> {

    private final String[] annotations;

    @SuppressWarnings("unchecked")
    public HasAnnotationMatcher(Class<? extends Annotation> ... classes) {
      this(X_Source.toStringCanonical(classes));
    }
    public HasAnnotationMatcher(String ... annotations) {
      this.annotations = annotations;
    }

    @Override
    public Boolean io(ClassFile value) {
      for (String annotation : annotations) {
        if (value.getAnnotation(annotation) != null) {
          return true;
        }
      }
      return false;
    }

  }

}

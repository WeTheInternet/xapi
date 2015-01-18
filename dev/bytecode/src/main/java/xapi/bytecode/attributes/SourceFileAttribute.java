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
package xapi.bytecode.attributes;

import java.io.DataInput;
import java.io.IOException;
import java.util.Map;

import xapi.bytecode.ConstPool;
import xapi.util.X_Byte;

public class SourceFileAttribute extends AttributeInfo {
  /**
   * The name of this attribute <code>"SourceFile"</code>.
   */
  public static final String tag = "SourceFile";

  SourceFileAttribute(ConstPool cp, int n, DataInput in)
      throws IOException
  {
      super(cp, n, in);
  }

  /**
   * Constructs a SourceFile attribute.
   *
   * @param cp                a constant pool table.
   * @param filename          the name of the source file.
   */
  public SourceFileAttribute(ConstPool cp, String filename) {
      super(cp, tag);
      int index = cp.addUtf8Info(filename);
      byte[] bvalue = new byte[2];
      bvalue[0] = (byte)(index >>> 8);
      bvalue[1] = (byte)index;
      set(bvalue);
  }

  /**
   * Returns the file name indicated by <code>sourcefile_index</code>.
   */
  public String getFileName() {
      return getConstPool().getUtf8Info(X_Byte.readU16bit(get(), 0));
  }

  /**
   * Makes a copy.  Class names are replaced according to the
   * given <code>Map</code> object.
   *
   * @param newCp     the constant pool table used by the new copy.
   * @param classnames        pairs of replaced and substituted
   *                          class names.
   */
  @Override
  public AttributeInfo copy(ConstPool newCp, Map<?, ?> classnames) {
      return new SourceFileAttribute(newCp, getFileName());
  }
}
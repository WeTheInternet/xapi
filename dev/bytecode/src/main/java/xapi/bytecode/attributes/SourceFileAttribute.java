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
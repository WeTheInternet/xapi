/**
 *
 */
package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


class MethodTypeInfo extends ConstInfo {
  static final int tag = 16;
  int descriptor;

  public MethodTypeInfo(final DataInput in, final int index) throws IOException {
    descriptor = in.readUnsignedShort();
  }

  public MethodTypeInfo(final int desc, final int index) {
    descriptor = desc;
  }

  @Override
  public int copy(final ConstPool src, final ConstPool dest, final Map map) {
    String desc = src.getUtf8Info(descriptor);
    desc = Descriptor.rename(desc, map);
    return dest.addMethodTypeInfo(dest.addUtf8Info(desc));
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof MethodTypeInfo) {
      return ((MethodTypeInfo)obj).descriptor == descriptor;
    } else {
      return false;
    }
  }

  @Override
  public int getTag() { return tag; }

  @Override
  public int hashCode() { return descriptor; }

  @Override
  public void print(final PrintWriter out) {
    out.print("MethodType #");
    out.println(descriptor);
  }

  public void renameClass(final ConstPool cp, final Map map, final HashMap cache) {
    final String desc = cp.getUtf8Info(descriptor);
    final String desc2 = Descriptor.rename(desc, map);
    if (desc != desc2) {
      if (cache == null) {
        descriptor = cp.addUtf8Info(desc2);
      } else {
        cache.remove(this);
        descriptor = cp.addUtf8Info(desc2);
        cache.put(this, this);
      }
    }
  }

  public void renameClass(final ConstPool cp, final String oldName, final String newName, final HashMap cache) {
    final String desc = cp.getUtf8Info(descriptor);
    final String desc2 = Descriptor.rename(desc, oldName, newName);
    if (desc != desc2) {
      if (cache == null) {
        descriptor = cp.addUtf8Info(desc2);
      } else {
        cache.remove(this);
        descriptor = cp.addUtf8Info(desc2);
        cache.put(this, this);
      }
    }
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    out.writeByte(tag);
    out.writeShort(descriptor);
  }
}

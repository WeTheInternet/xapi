/**
 *
 */
package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;


class InvokeDynamicInfo extends ConstInfo {
  static final int tag = 18;
  int bootstrap, nameAndType;

  public InvokeDynamicInfo(final DataInput in, final int index) throws IOException {
    bootstrap = in.readUnsignedShort();
    nameAndType = in.readUnsignedShort();
  }

  public InvokeDynamicInfo(final int bootstrapMethod, final int ntIndex, final int index) {
    bootstrap = bootstrapMethod;
    nameAndType = ntIndex;
  }

  public int copy(final ConstPool src, final ConstPool dest, final Map map) {
    return dest.addInvokeDynamicInfo(bootstrap,
        src.getItem(nameAndType).copy(src, dest, map));
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof InvokeDynamicInfo) {
      final InvokeDynamicInfo iv = (InvokeDynamicInfo)obj;
      return iv.bootstrap == bootstrap && iv.nameAndType == nameAndType;
    } else {
      return false;
    }
  }

  @Override
  public int getTag() { return tag; }

  @Override
  public int hashCode() { return bootstrap << 16 ^ nameAndType; }

  public void print(final PrintWriter out) {
    out.print("InvokeDynamic #");
    out.print(bootstrap);
    out.print(", name&type #");
    out.println(nameAndType);
  }

  public void write(final DataOutput out) throws IOException {
    out.writeByte(tag);
    out.writeShort(bootstrap);
    out.writeShort(nameAndType);
  }
}
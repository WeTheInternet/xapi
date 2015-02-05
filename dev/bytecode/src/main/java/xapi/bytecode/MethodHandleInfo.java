/**
 *
 */
package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;



class MethodHandleInfo extends ConstInfo {
  static final int tag = 15;
  int refKind, refIndex;

  public MethodHandleInfo(final DataInput in, final int index) throws IOException {
    refKind = in.readUnsignedByte();
    refIndex = in.readUnsignedShort();
  }

  public MethodHandleInfo(final int kind, final int referenceIndex, final int index) {
    refKind = kind;
    refIndex = referenceIndex;
  }

  @Override
  public int copy(final ConstPool src, final ConstPool dest, final Map map) {
    return dest.addMethodHandleInfo(refKind,
        src.getItem(refIndex).copy(src, dest, map));
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof MethodHandleInfo) {
      final MethodHandleInfo mh = (MethodHandleInfo)obj;
      return mh.refKind == refKind && mh.refIndex == refIndex;
    } else {
      return false;
    }
  }

  @Override
  public int getTag() { return tag; }

  @Override
  public int hashCode() { return refKind << 16 ^ refIndex; }

  @Override
  public void print(final PrintWriter out) {
    out.print("MethodHandle #");
    out.print(refKind);
    out.print(", index #");
    out.println(refIndex);
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    out.writeByte(tag);
    out.writeByte(refKind);
    out.writeShort(refIndex);
  }
}
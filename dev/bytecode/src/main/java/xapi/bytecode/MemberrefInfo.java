package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public abstract class MemberrefInfo extends ConstInfo {
    int classIndex;
    int nameAndTypeIndex;

    public MemberrefInfo(int cindex, int ntindex) {
        classIndex = cindex;
        nameAndTypeIndex = ntindex;
    }

    public MemberrefInfo(DataInput in) throws IOException {
        classIndex = in.readUnsignedShort();
        nameAndTypeIndex = in.readUnsignedShort();
    }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        int classIndex2 = src.getItem(classIndex).copy(src, dest, map);
        int ntIndex2 = src.getItem(nameAndTypeIndex).copy(src, dest, map);
        return copy2(dest, classIndex2, ntIndex2);
    }

    @Override
    boolean hashCheck(int a, int b) { return a == classIndex && b == nameAndTypeIndex; }

    abstract protected int copy2(ConstPool dest, int cindex, int ntindex);

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(getTag());
        out.writeShort(classIndex);
        out.writeShort(nameAndTypeIndex);
    }

    @Override
    public void print(PrintWriter out) {
        out.print(getTagName() + " #");
        out.print(classIndex);
        out.print(", name&type #");
        out.println(nameAndTypeIndex);
    }

    public abstract String getTagName();
}
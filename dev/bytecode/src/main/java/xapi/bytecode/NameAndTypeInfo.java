package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

class NameAndTypeInfo extends ConstInfo {
    static final int tag = 12;
    int memberName;
    int typeDescriptor;

    public NameAndTypeInfo(int name, int type) {
        memberName = name;
        typeDescriptor = type;
    }

    public NameAndTypeInfo(DataInput in) throws IOException {
        memberName = in.readUnsignedShort();
        typeDescriptor = in.readUnsignedShort();
    }

    @Override
    boolean hashCheck(int a, int b) { return a == memberName && b == typeDescriptor; }

    @Override
    public int getTag() { return tag; }

    @Override
    public void renameClass(ConstPool cp, String oldName, String newName) {
        String type = cp.getUtf8Info(typeDescriptor);
        String type2 = Descriptor.rename(type, oldName, newName);
        if (type != type2) {
          typeDescriptor = cp.addUtf8Info(type2);
        }
    }

    @Override
    public void renameClass(ConstPool cp, Map<?, ?> map) {
        String type = cp.getUtf8Info(typeDescriptor);
        String type2 = Descriptor.rename(type, map);
        if (type != type2) {
          typeDescriptor = cp.addUtf8Info(type2);
        }
    }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        String mname = src.getUtf8Info(memberName);
        String tdesc = src.getUtf8Info(typeDescriptor);
        tdesc = Descriptor.rename(tdesc, map);
        return dest.addNameAndTypeInfo(dest.addUtf8Info(mname),
                                       dest.addUtf8Info(tdesc));
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(tag);
        out.writeShort(memberName);
        out.writeShort(typeDescriptor);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("NameAndType #");
        out.print(memberName);
        out.print(", type #");
        out.println(typeDescriptor);
    }
}
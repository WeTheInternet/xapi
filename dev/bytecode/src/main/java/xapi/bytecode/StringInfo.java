package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class StringInfo extends ConstInfo {
    static final int tag = 8;
    int string;

    public StringInfo(int str) {
        string = str;
    }

    public StringInfo(DataInput in) throws IOException {
        string = in.readUnsignedShort();
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        return dest.addStringInfo(src.getUtf8Info(string));
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(tag);
        out.writeShort(string);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("String #");
        out.println(string);
    }
}
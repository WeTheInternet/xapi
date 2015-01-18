package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class Utf8Info extends ConstInfo {
    static final int tag = 1;
    String string;
    int index;

    public Utf8Info(String utf8, int i) {
        string = utf8;
        index = i;
    }

    public Utf8Info(DataInput in, int i) throws IOException {
        string = in.readUTF();
        index = i;
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        return dest.addUtf8Info(string);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(tag);
        out.writeUTF(string);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("UTF8 \"");
        out.print(string);
        out.println("\"");
    }
}
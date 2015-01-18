package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class LongInfo extends ConstInfo {
    static final int tag = 5;
    long value;

    public LongInfo(long l) {
        value = l;
    }

    public LongInfo(DataInput in) throws IOException {
        value = in.readLong();
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        return dest.addLongInfo(value);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(tag);
        out.writeLong(value);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("Long ");
        out.println(value);
    }
}
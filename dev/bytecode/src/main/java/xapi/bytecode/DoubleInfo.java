package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class DoubleInfo extends ConstInfo {
    static final int tag = 6;
    double value;

    public DoubleInfo(double d) {
        value = d;
    }

    public DoubleInfo(DataInput in) throws IOException {
        value = in.readDouble();
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        return dest.addDoubleInfo(value);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(tag);
        out.writeDouble(value);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("Double ");
        out.println(value);
    }
}
package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class FloatInfo extends ConstInfo {
    static final int tag = 4;
    float value;

    public FloatInfo(float f) {
        value = f;
    }

    public FloatInfo(DataInput in) throws IOException {
        value = in.readFloat();
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        return dest.addFloatInfo(value);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(tag);
        out.writeFloat(value);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("Float ");
        out.println(value);
    }
}
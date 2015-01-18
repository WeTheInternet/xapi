package xapi.bytecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class IntegerInfo extends ConstInfo {
    static final int tag = 3;
    int value;

    public IntegerInfo(int i) {
        value = i;
    }

    public IntegerInfo(DataInput in) throws IOException {
        value = in.readInt();
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        return dest.addIntegerInfo(value);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeByte(tag);
        out.writeInt(value);
    }

    @Override
    public void print(PrintWriter out) {
        out.print("Integer ");
        out.println(value);
    }
}
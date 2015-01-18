package xapi.bytecode;

import java.io.DataInput;
import java.io.IOException;

public class MethodrefInfo extends MemberrefInfo {
    static final int tag = 10;

    public MethodrefInfo(int cindex, int ntindex) {
        super(cindex, ntindex);
    }

    public MethodrefInfo(DataInput in) throws IOException {
        super(in);
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public String getTagName() { return "Method"; }

    @Override
    protected int copy2(ConstPool dest, int cindex, int ntindex) {
        return dest.addMethodrefInfo(cindex, ntindex);
    }
}
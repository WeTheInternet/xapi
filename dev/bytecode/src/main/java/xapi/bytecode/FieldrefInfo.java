package xapi.bytecode;

import java.io.DataInput;
import java.io.IOException;

public class FieldrefInfo extends MemberrefInfo {
    static final int tag = 9;

    public FieldrefInfo(int cindex, int ntindex) {
        super(cindex, ntindex);
    }

    public FieldrefInfo(DataInput in) throws IOException {
        super(in);
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public String getTagName() { return "Field"; }

    @Override
    protected int copy2(ConstPool dest, int cindex, int ntindex) {
        return dest.addFieldrefInfo(cindex, ntindex);
    }
}
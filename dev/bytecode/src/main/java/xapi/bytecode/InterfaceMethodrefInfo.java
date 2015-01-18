package xapi.bytecode;

import java.io.DataInput;
import java.io.IOException;

public class InterfaceMethodrefInfo extends MemberrefInfo {
    static final int tag = 11;

    public InterfaceMethodrefInfo(int cindex, int ntindex) {
        super(cindex, ntindex);
    }

    public InterfaceMethodrefInfo(DataInput in) throws IOException {
        super(in);
    }

    @Override
    public int getTag() { return tag; }

    @Override
    public String getTagName() { return "Interface"; }

    @Override
    protected int copy2(ConstPool dest, int cindex, int ntindex) {
        return dest.addInterfaceMethodrefInfo(cindex, ntindex);
    }
}
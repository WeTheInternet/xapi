package xapi.bytecode;

import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/* padding following DoubleInfo or LongInfo.
 */
public class ConstInfoPadding extends ConstInfo {
    @Override
    public int getTag() { return 0; }

    @Override
    public int copy(ConstPool src, ConstPool dest, Map<?, ?> map) {
        return dest.addConstInfoPadding();
    }

    @Override
    public void write(DataOutput out) throws IOException {}

    @Override
    public void print(PrintWriter out) {
        out.println("padding");
    }
}